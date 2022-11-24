/*
 * Copyright (c) 2022 ifly6
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this class file and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.git.ifly6.communique.data;

import com.git.ifly6.commons.CommuniqueSplitter;
import com.git.ifly6.nsapi.NSIOException;
import com.git.ifly6.nsapi.NSRegion;
import com.git.ifly6.nsapi.NSWorld;
import com.git.ifly6.nsapi.ctelegram.io.cache.CommDelegatesCache;
import com.git.ifly6.nsapi.ctelegram.io.cache.CommMembersCache;
import com.git.ifly6.nsapi.ctelegram.io.cache.CommRegionCache;
import com.git.ifly6.nsapi.ctelegram.io.cache.CommRegionTagCache;
import com.git.ifly6.nsapi.ctelegram.monitors.CommMonitor;
import com.git.ifly6.nsapi.ctelegram.monitors.rules.CommWaitingMonitor;
import com.git.ifly6.nsapi.ctelegram.monitors.updaters.CommActiveMonitor;
import com.git.ifly6.nsapi.ctelegram.monitors.updaters.CommApprovalMonitor;
import com.git.ifly6.nsapi.ctelegram.monitors.updaters.CommApprovalRaidMonitor;
import com.git.ifly6.nsapi.ctelegram.monitors.updaters.CommMovementMonitor;
import com.git.ifly6.nsapi.ctelegram.monitors.updaters.CommRecruitMonitor;
import com.git.ifly6.nsapi.ctelegram.monitors.updaters.CommVoteMonitor;
import com.git.ifly6.nsapi.telegram.JTelegramException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Defines a number of recipient types and provides methods to decompose those types into lists of
 * <code>CommuniqueRecipient</code>.
 * @author ifly6
 * @since version 2.0 (build 7)
 */
public enum CommuniqueRecipientType {

    /**
     * Declares the recipient is a nation and requires no further processing in decomposition.
     * @since version 2.0 (build 7)
     */
    NATION {
        @Override
        public List<CommuniqueRecipient> decompose(CommuniqueRecipient cr) {
            return Collections.singletonList(cr); // return singleton list
        }
    },

    // This code block must be before the REGION code block otherwise it will get substring matched over :(
    /**
     * Declares that the recipient is a REGION TAG and that it needs decomposing into a list of regions which then is
     * decomposed into the nations therein.
     * @since version 2.4 (build 11) (2020-04-04)
     */
    REGION_TAG {
        @Override
        public List<CommuniqueRecipient> decompose(CommuniqueRecipient cr) throws JTelegramException {
            List<String> regions = CommRegionTagCache.getInstance().getRegionsWithTag(cr.getName());
//            LOGGER.info(String.format("Tag %s: %d regions", cr.getName(), regions.size()));
            return regions.stream()
                    .map(s -> new CommuniqueRecipient(cr.getFilterType(), CommuniqueRecipientType.REGION, s))
                    .map(CommuniqueRecipient::decompose)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }
    },

    /**
     * Declares the recipient is a region, allowing for decomposition into a list of {@link CommuniqueRecipient} nations
     * in the region.
     * @since version 2.0 (build 7)
     */
    REGION {
        @Override
        public List<CommuniqueRecipient> decompose(CommuniqueRecipient cr) throws JTelegramException {
            NSRegion region = CommRegionCache.getInstance().lookupObject(cr.getName());
            List<String> regionMembers = region.getRegionMembers();

//            LOGGER.info(String.format("Region %s: %d nations", cr.getName(), regionMembers.size()));
            return newRecipients(regionMembers, cr.getFilterType());
        }
    },

    /**
     * Declares the recipient is one of various tags, which can be used to get the members of the World Assembly,
     * delegates thereof, or new nations.
     * @since version 2.0 (build 7)
     */
    TAG {
        @Override
        public List<CommuniqueRecipient> decompose(CommuniqueRecipient cr) throws JTelegramException {
            String tag = cr.getName();
            if (tag.equals("wa"))
                return newRecipients(CommMembersCache.getInstance().getWAMembers(), cr.getFilterType());
            if (tag.equals("delegates"))
                return newRecipients(CommDelegatesCache.getInstance().getDelegates(), cr.getFilterType());
            if (tag.equals("new"))
                try {
                    return newRecipients(NSWorld.getNew(), cr.getFilterType());
                } catch (NSIOException e) { // in this case, we want it to ignore all errors
                    LOGGER.info("failed to get new recipients from tag:new");
                    return Collections.emptyList();
                }

            throw newException(cr);
        }
    },

    /**
     * Provides from {@link CommRecruitMonitor} new nations. See {@link CommRecruitMonitor#setUpdateInterval(Duration)}
     * and {@link CommRecruitMonitor#getInstance()}. The number of nations returned should be specified as in {@code
     * _new:5}. This monitor never returns the same nation twice; it does not exhaust.
     * @since version 3.0 (build 13)
     */
    _NEW {
        @Override
        public List<CommuniqueRecipient> decompose(CommuniqueRecipient cr) {
            String tag = cr.getName();
            try {
                int limit = Integer.parseInt(tag);
                CommMonitor nnm = new CommWaitingMonitor(CommRecruitMonitor.getInstance().setBatchLimit(limit));
                return newRecipients(nnm.getRecipients(), cr.getFilterType());

            } catch (NumberFormatException e) {
                throw newException(cr);
            }
        }
    },

    /**
     * Yields filters related to the NationStates Happenings. Valid input includes the following.
     * <ul>
     *     <li>{@code _happenings:active} (see {@link CommActiveMonitor}) returning nations active in the last
     *     10 minutes.</li>
     * </ul>
     * @since version 3.0 (build 13)
     */
    _HAPPENINGS {
        @Override
        public List<CommuniqueRecipient> decompose(CommuniqueRecipient cr) {
            String tag = cr.getName();
            if (tag.equals("active")) { // active nations
                return newRecipients(
                        CommActiveMonitor.getInstance().getRecipients(),
                        cr.getFilterType());
            }

            throw newException(cr);
        }
    },

    /**
     * Nations moving into or out of a list of regions. Eg, {@code _movement:into;europe,the_north_pacific} or {@code
     * _movement:out_of;europe}.
     * @since version 3.0 (build 13)
     */
    _MOVEMENT {
        private final CommuniqueSplitter splitter = new CommuniqueSplitter(this.toString(), 2);

        @Override
        public List<CommuniqueRecipient> decompose(CommuniqueRecipient cr) {
            String[] tags = splitter.split(cr.getName());
            String direction = tags[0];
            String region = tags[1];
            CommMonitor monitor = new CommWaitingMonitor(CommMovementMonitor.getOrCreate(direction, region));
            return newRecipients(monitor.getRecipients(), cr.getFilterType());
        }
    },

    /**
     * Delegates approving a proposal. Eg, {@code _approval:__raids__} for all approval raids, {@code
     * _approval:given_to; PROPOSAL_ID}, or {@code _approval:removed_from; PROPOSAL_ID}.
     * @since version 3.0 (build 13)
     */
    _APPROVAL {
        private final CommuniqueSplitter splitter = new CommuniqueSplitter(this.toString(), 2);

        @Override
        public List<CommuniqueRecipient> decompose(CommuniqueRecipient cr) {
            if (cr.getName().equalsIgnoreCase("__raids__"))
                return newRecipients(
                        new CommWaitingMonitor(
                                CommApprovalRaidMonitor.getInstance()).getRecipients(),
                        cr.getFilterType());

            String[] tags = splitter.split(cr.getName());
            String givenOrRemoved = tags[0];
            String proposal = tags[1];
            CommMonitor monitor = new CommWaitingMonitor(CommApprovalMonitor.getOrCreate(givenOrRemoved, proposal));
            return newRecipients(monitor.getRecipients(), cr.getFilterType());
        }
    },

    /**
     * Persons voting on a proposal. Eg {@code _voting:ga; for}.
     * @since version 3.0 (build 13)
     */
    _VOTING {
        private final CommuniqueSplitter splitter = new CommuniqueSplitter(this.toString(), 2);

        @Override
        public List<CommuniqueRecipient> decompose(CommuniqueRecipient cr) {
            String[] split = splitter.split(cr.getName());
            String chamber = split[0];
            String voting = split[1];
            CommMonitor monitor = new CommWaitingMonitor(CommVoteMonitor.getOrCreate(chamber, voting));
            return newRecipients(monitor.getRecipients(), cr.getFilterType());
        }
    },

    /** Declares that the recipient has no recipient type. This element is parsed last. */
    NONE {
        @Override
        public List<CommuniqueRecipient> decompose(CommuniqueRecipient cr) { return Collections.emptyList(); }

        @Override
        public String toString() { return ""; }
    };

    private static final Logger LOGGER = Logger.getLogger(CommuniqueRecipientType.class.getName());

    /** Recipient type prefixes should be compatible with the NationStates telegram system. */
    @Override
    public String toString() { return this.name().toLowerCase(); }

    /**
     * Decomposes tag into {@code List<{@link CommuniqueRecipient}>}.
     * @param cr tag to be decomposed
     * @return list of recipients
     */
    public abstract List<CommuniqueRecipient> decompose(CommuniqueRecipient cr);

    /**
     * Translates nation reference names into {@link CommuniqueRecipientType#NATION} {@code CommuniqueRecipient}s.
     * @param list       of nation reference names
     * @param filterType from which to extract filter type data
     * @return list of recipients
     */
    private static List<CommuniqueRecipient> newRecipients(List<String> list, CommuniqueFilterType filterType) {
        // we use this a lot, probably better to use a loop for speed
        List<CommuniqueRecipient> result = new ArrayList<>(list.size());
        for (String s : list)
            result.add(CommuniqueRecipients.createNation(filterType, s));
        return result;
    }

    /** Creates illegal argument exception with preformatted string. */
    private static IllegalArgumentException newException(CommuniqueRecipient s) {
        return new IllegalArgumentException(String.format("Invalid tag string \"%s\"", s));
    }
}
