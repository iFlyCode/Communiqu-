/*
 * Copyright (c) 2015 ifly6
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/**
 * Classes related to the implementation of the {@link Communiqué Communiqué} recipients system, specifications,
 * and starting classes for its implementation.
 *
 * <p> The package contains five classes: <code>Communiqué</code>, which launches the GUI and
 * processes the data; {@link CommuniquéFileReader CommuniquéFileReader}, which checks version, comments, and
 * loads the relevant data into RAM; {@link CommuniquéFileWriter CommuniquéFileWriter}, which writes the
 * files which <code>CommuniquéFileReader</code> will have to read;
 * {@link CommuniquéParser CommuniquéParser}, whose parsing system defines the grammar of the Communiqué
 * recipients language (and processes that information); and finally,
 * {@link CommuniquéLogger CommuniquéLogger}, which interfaces with JavaTelegram to return relevant
 * information to Communiqué.
 */
package com.git.ifly6.communique;