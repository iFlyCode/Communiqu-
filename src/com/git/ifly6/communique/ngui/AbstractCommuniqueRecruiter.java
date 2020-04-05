/* Copyright (c) 2018 Kevin Wong. All Rights Reserved. */
package com.git.ifly6.communique.ngui;

import com.git.ifly6.communique.CommuniqueUtilities;
import com.git.ifly6.communique.data.Communique7Parser;
import com.git.ifly6.communique.data.CommuniqueRecipient;
import com.git.ifly6.communique.data.CommuniqueRecipients;
import com.git.ifly6.communique.data.FilterType;
import com.git.ifly6.communique.data.RecipientType;
import com.git.ifly6.communique.io.CommuniqueConfig;
import com.git.ifly6.marconi.MarconiRecruiter;
import com.git.ifly6.nsapi.NSException;
import com.git.ifly6.nsapi.NSIOException;
import com.git.ifly6.nsapi.NSNation;
import com.git.ifly6.nsapi.telegram.JTelegramException;
import com.git.ifly6.nsapi.telegram.JTelegramLogger;
import com.git.ifly6.nsapi.telegram.util.JInfoFetcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Provides the outline for the recruiter classes. Also provides recipient search functionality shared between {@link
 * CommuniqueRecruiter} and {@link MarconiRecruiter}.
 * @author ifly6
 */
public abstract class AbstractCommuniqueRecruiter implements JTelegramLogger {

	private static final JInfoFetcher fetcher = JInfoFetcher.instance();
	private static final Logger LOGGER = Logger.getLogger(AbstractCommuniqueRecruiter.class.getName());

	protected List<CommuniqueRecipient> filterList;
	protected LinkedHashSet<CommuniqueRecipient> sentList;
	protected Set<CommuniqueRecipient> proscribedRegions;

	public void setWithCConfig(CommuniqueConfig config) {
		// get the sent list first
		sentList = config.getcRecipients().stream()
				.filter(r -> r.getRecipientType() == RecipientType.NATION)
				.filter(r -> r.getFilterType() == FilterType.EXCLUDE)
				.collect(Collectors.toCollection(LinkedHashSet::new));

		// then get extra filter list things
		RecipientType[] goodRecipientTypes = {RecipientType.EMPTY, RecipientType.NATION};
		filterList = config.getcRecipients().stream()
				.filter(r -> r.getFilterType() != FilterType.NORMAL) // exclude all additions
				.filter(r -> Arrays.asList(goodRecipientTypes).contains(r.getRecipientType()))
				.filter(r -> !sentList.contains(r))
				.collect(Collectors.toList());
	}

	public abstract void send();

	@Override
	public void sentTo(String recipient, int recipientNum, int length) {
		sentList.add(CommuniqueRecipients.createExcludedNation(recipient));
	}

	/**
	 * Returns a recipient based on the new recipients list from the NS API, filtered by whether it is proscribed. Note
	 * that any issues or problems are dealt with my defaulting to the newest nation, ignoring the proscription filter.
	 * It also filters by whether the nation is recruitable.
	 * @return a <code>String</code> with the name of the recipient
	 */
	public CommuniqueRecipient getRecipient() {
		try {
			try {
				List<String> possibleRecipients = CommuniqueUtilities.ref(fetcher.getNew());
				for (String element : possibleRecipients) {

					// if in sent list, next
					// if otherwise prohibited by other filter rules, next
					// if not recruitable, next
					// otherwise, return

					LOGGER.info(String.format("Checking %s", element));
					Communique7Parser parser = new Communique7Parser().apply(CommuniqueRecipients.createNation(element))
							.apply(new ArrayList<>(sentList)) // sent list filter
							.apply(filterList); // other filters
					if (!parser.listRecipients().contains(element)) continue;

					try {
						NSNation prNation = new NSNation(element).populateData();
						if (!prNation.isRecruitable()) continue; // if not recruitable yeet
						if (isProscribed(prNation)) continue; // if proscribed yeet

					} catch (NSException e) {
						// if it doesn't exist or otherwise fails, ignore this one
						continue;
					}

					// 2017-03-18 proscription and recruit checks are now performed by JavaTelegram#predicates
					// 2020-01-26 disregard above, they're just not done by JavaTelegram#predicates
					LOGGER.info(String.format("Returning match %s", element));
					return CommuniqueRecipients.createNation(element);
				}

				// if the filtering failed entirely, then simply just return the newest nation.
				LOGGER.info(String.format("Could not find match; returning default match %s", possibleRecipients.get(0)));
				return CommuniqueRecipients.createNation(possibleRecipients.get(0));

			} catch (JTelegramException e) {
				LOGGER.warning("Cannot fetch new nations. Retrying. Sleep one second.");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ignored) {
				}
				return getRecipient();  // retry

			} catch (NSIOException e) {
				LOGGER.warning("NS API threw error when loading new nation data. Retrying.");
				return getRecipient();  // retry

			} catch (RuntimeException e) {
				LOGGER.warning("Unclear reason why we cannot load new nation data. Retrying.");
				return getRecipient();  // retry
			}

		} catch (StackOverflowError stackOverflow) {
			LOGGER.severe("Too many attempts to load nation data. Check your internet connection.");
			throw new NSIOException("Stack overflow error!");
		}
	}

	/**
	 * Determines whether a nation is in a region excluded by the JList <code>excludeList</code>. This method acts with
	 * two assumptions: (1) it is not all right to telegram to anyone who resides in a prescribed region and (2) if they
	 * moved out of the region since founding, it is certainly all right to do so.
	 * @param nation to check
	 * @return <code>boolean</code> on whether it is proscribed
	 */
	private boolean isProscribed(NSNation nation) {

		if (!nation.hasData()) nation.populateData();

		// API gives region names, can only do this by converting to ref names and then comparing
		String nRegion = CommuniqueUtilities.ref(nation.getRegion());
		return proscribedRegions.stream()
				.map(CommuniqueRecipient::getName)
				.map(CommuniqueUtilities::ref)
				.anyMatch(regionName -> regionName.equals(nRegion));

	}

}