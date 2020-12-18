/*
 * Copyright (c) 2020 ifly6
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

package com.git.ifly6.nsapi.ctelegram.monitors;

import com.git.ifly6.nsapi.ctelegram.io.CommWorldAssembly;
import org.apache.commons.lang3.Range;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Monitors delegates who <b>cast</b> or <b>are</b> (depending on {@code ignoreInitial}, see {@link *
 * #CommDelegateVoteMonitor(CommWorldAssembly.Chamber, CommWorldAssembly.Vote, boolean)}) voting a certain way in the
 * specified chamber.
 * <p>As a concrete example. If monitoring AYE votes in the GA while ignoring initial voters this will <b>not</b> track
 * anyone who votes NAY. Nor will it mark anyone who is currently voting AYE and switches their vote to NAY. However, it
 * will mark anyone who casts a new vote AYE.</p>
 * @since version 3.0 (build 13)
 */
public class CommDelegateVoteMonitor extends CommAssemblyMonitor implements CommMonitor {

    public static final Logger LOGGER = Logger.getLogger(CommDelegateVoteMonitor.class.getName());

    private final Range<Integer> weightRange;

    /**
     * @param chamber       to check for voters
     * @param voting        direction; choose one
     * @param ignoreInitial if true, does not include people who already voted
     */
    public CommDelegateVoteMonitor(CommWorldAssembly.Chamber chamber, CommWorldAssembly.Vote voting,
                                   Range<Integer> weightRange, boolean ignoreInitial) {
        this.chamber = chamber;
        this.voting = voting;
        this.weightRange = weightRange;

        if (ignoreInitial) {
            Set<String> voters = new HashSet<>(getMonitoredRecipients());
            previousVoters = voters;
            currentVoters = voters;
        }
    }

    /**
     * Gets list of delegates falling within specifications.
     * @return list of monitored delegates
     */
    @Override
    protected Set<String> getMonitoredRecipients() {
        return CommWorldAssembly.getDelegates(this.chamber, this.voting).stream()
                .filter(d -> this.weightRange.contains(d.weight))      // require range contains
                .map(CommWorldAssembly.Delegate::getName)   // map
                .collect(Collectors.toCollection(HashSet::new));
    }
}