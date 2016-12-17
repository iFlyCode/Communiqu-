/* Copyright (c) 2016 ifly6
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. */
package com.git.ifly6.communique.data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

/** <code>Communique7Parser</code> is the new parser designed for Communique 7, which implements the same way to declare
 * recipients as used in NationStates. It supersedes the old parser, {@link CommuniqueParser}, which used the custom
 * recipient declaration system in older versions of Communique.
 * <p>
 * <code>Communique7Parser</code> also provides methods to translate between the old and new Communique address tokens,
 * allowing for a seam-less transition between the old and new token systems.
 * </p>
 * @author ifly6 */
public class Communique7Parser {

	List<CommuniqueRecipient> recipients = new ArrayList<>(0);
	
	/** Creates a new empty parser without any applied tokens. To actually use the parser, apply tokens from here,
	 * either in the form of a <code>List&lt;String&gt;</code> or <code>CommuniqueRecipient</code>. */
	public Communique7Parser() {
	}
	
	/** Applies the tokens, specified in the <code>CommuniqueRecipient</code> object, to the recipients list in the
	 * parser.
	 * @param token a <code>CommuniqueRecipient</code>
	 * @return this parser for further token applications, if necessary */
	public Communique7Parser apply(CommuniqueRecipient token) {
		recipients = token.getFilterType().apply(recipients, token);
		/* This is the beautiful part, because I've chained everything to a filter, this means that I don't have to
		 * write any code whatsoever to sort things into what they have to do, unlike the old parser. Now, everything is
		 * chained to an ENUM which already knows exactly what it has to do, and therefore, can do it easily. */
		return this;
	}

	/** Applies the tokens to the recipients list with a specified list of tokens. All of these should be parse-able
	 * <code>CommuniqueRecipient</code>s.
	 * @param list of parse-able <code>CommuniqueRecipient</code>s
	 * @return this object for further applications, if necessary */
	public Communique7Parser apply(List<String> list) {
		list.stream().filter(s -> !s.startsWith("#")).filter(s -> !StringUtils.isEmpty(s))
				.map(CommuniqueRecipient::parseRecipient).forEach(this::apply);
		return this;
	}

	/** Applies tokens based on a variable number of <code>CommuniqueRecipient</code>s.
	 * @param tokens to apply
	 * @return this parser for further analysis if necessary */
	public Communique7Parser apply(CommuniqueRecipient... tokens) {
		Stream.of(tokens).forEach(this::apply);
		return this;
	}
	
	/** Returns all of the recipients in the standard NationStates reference name form in a <code>List</code>.
	 * @return a list of all recipients in standard reference name form */
	public List<String> getRecipients() {
		return recipients.stream().map(CommuniqueRecipient::getName).collect(Collectors.toList());
	}

	/** Translates a number of old tokens into the new Communique 7 tokens.
	 * @param oldTokens to translate
	 * @return a list of tokens which means the same thing in the new system */
	public static List<String> translateTokens(List<String> oldTokens) {
		List<String> tokens = new ArrayList<>();
		for (String oldToken : oldTokens) {
			if (oldToken.startsWith("flag:recruit")) {
				tokens.add(oldToken);
				continue;
			}
			if (oldToken.contains("->")) {
				String[] split = oldToken.split("->");
				tokens.add(translateToken(split[0]));
				tokens.add(translateToken("->" + split[1]));
				continue;
			}
			if (oldToken.contains("--")) {
				String[] split = oldToken.split("--");
				tokens.add(translateToken(split[0]));
				tokens.add(translateToken("--" + split[1]));
				continue;
			}
			tokens.add(translateToken(oldToken));
			
		}
		return tokens;
	}

	/** Translates a single token from the old system to the new Communique 7 system
	 * @param oldToken in a <code>String</code> form, like "wa:delegates"
	 * @return the token in the Communique 7 form, which, for "wa:delegates", would turn into "tag:delegates" */
	public static String translateToken(String oldToken) {
		
		// logic tags, somewhat recursive to ease writing
		if (oldToken.startsWith("/")) { return "-" + translateToken(oldToken.replaceFirst("/", "").trim()); }
		if (oldToken.startsWith("--")) { return "-" + translateToken(oldToken.replace("--", "").trim()); }
		if (oldToken.startsWith("->")) { return "+" + translateToken(oldToken.replace("->", "").trim()); }

		// decomposition tags
		if (oldToken.equalsIgnoreCase("wa:delegates")) { return "tag:delegates"; }
		if (oldToken.equalsIgnoreCase("wa:members") || oldToken.equalsIgnoreCase("wa:nations")) { return "tag:wa"; }
		if (oldToken.startsWith("world:new")) { return "tag:new"; }

		// recipient tags
		if (oldToken.startsWith("region:")) { return oldToken; }
		return "nation:" + oldToken;

	}
}