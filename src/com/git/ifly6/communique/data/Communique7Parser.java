/* Copyright (c) 2018 ifly6. All Rights Reserved. */
package com.git.ifly6.communique.data;

import com.git.ifly6.javatelegram.JTelegramException;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <code>Communique7Parser</code> is the new parser designed for Communique 7, which implements the same way to declare
 * recipients as used in NationStates. It supersedes the old parser, {@link CommuniqueParser}, which used the custom
 * recipient declaration system in older versions of Communique.
 * <p><code>Communique7Parser</code> also provides methods to translate between the old and new Communique address
 * tokens, allowing for a seamless transition between the old and new token systems.</p>
 * <p>This class does not lazily load data. When invoking <code>apply</code>, all elements are processed
 * immediately. This class is meant to be used fluently, e.g.
 * <code>new Communique7Parser().apply(tokens).listRecipients()</code>.</p>
 * @author ifly6
 */
public class Communique7Parser {

	/**
	 * Declares the version of the parser, which is based on two values: (1) the syntax of the Communique recipients
	 * language and (2) the file syntax in which that information is held.
	 */
	public static final int version = 9;

	/** List of recipients changed by various actions and applications called by the parser. */
	private Set<CommuniqueRecipient> recipients;

	/**
	 * Creates a new empty parser without any applied tokens. To actually use the parser, apply tokens using the apply
	 * methods, either in the form of a <code>List&lt;String&gt;</code> or any number of
	 * <code>CommuniqueRecipient</code>.
	 */
	public Communique7Parser() {
		recipients = new LinkedHashSet<>();
	}

	/**
	 * Applies the tokens, specified in the <code>CommuniqueRecipient</code> object, to the recipients list in the
	 * parser.
	 * @param token a <code>CommuniqueRecipient</code>
	 * @return this parser
	 */
	public Communique7Parser apply(CommuniqueRecipient token) throws JTelegramException {
		recipients = token.getFilterType().apply(recipients, token);
		/* This is the beautiful part, because I've chained everything to a filter, this means that I don't have to
		 * write any code whatsoever to sort things into what they have to do, unlike the old parser. Now, everything is
		 * chained to an ENUM which already knows exactly what it has to do, and therefore, everything is already dealt
		 * with. */
		return this;
	}

	/**
	 * Applies the tokens to the recipients list with a specified list of tokens.
	 * @param list of <code>CommuniqueRecipient</code>s
	 * @return this parser
	 */
	public Communique7Parser apply(List<CommuniqueRecipient> list) throws JTelegramException {
		list.forEach(this::apply);
		return this;
	}

	/**
	 * Applies tokens based on a variable number of <code>CommuniqueRecipient</code>s.
	 * @param tokens to apply
	 * @return this parser
	 */
	public Communique7Parser apply(CommuniqueRecipient... tokens) {
		Arrays.stream(tokens).forEach(this::apply);
		return this;
	}

	/**
	 * Returns a list of all the recipients in standard NationStates reference name form
	 * @return list of recipients
	 */
	public List<String> listRecipients() {
		return recipients.stream()
				.map(CommuniqueRecipient::getName)
				.collect(Collectors.toList());
	}

}
