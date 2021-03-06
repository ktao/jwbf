/*
 * Copyright 2007 Tobias Knerr.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * Contributors:
 * Tobias Knerr
 *
 */
package net.sourceforge.jwbf.mediawiki.actions.queries;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.jwbf.core.RequestBuilder;
import net.sourceforge.jwbf.core.actions.Get;
import net.sourceforge.jwbf.core.actions.util.HttpAction;
import net.sourceforge.jwbf.mediawiki.ApiRequestBuilder;
import net.sourceforge.jwbf.mediawiki.actions.MediaWiki;
import net.sourceforge.jwbf.mediawiki.actions.MediaWiki.Version;
import net.sourceforge.jwbf.mediawiki.actions.util.MWAction;
import net.sourceforge.jwbf.mediawiki.actions.util.RedirectFilter;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;

import com.google.common.collect.Lists;

/**
 * action class using the MediaWiki-api's "list=backlinks".
 * 
 * @author Thomas Stock
 * @author Tobias Knerr
 * @since JWBF 1.1
 */
@Slf4j
public class BacklinkTitles extends TitleQuery<String> {

  /**
   * enum that defines the three posibilities of dealing with article lists including both redirects
   * and non-redirects.
   * <ul>
   * <li>all: List all pages regardless of their redirect flag</li>
   * <li>redirects: Only list redirects</li>
   * <li>nonredirects: Don't list redirects</li>
   * </ul>
   */

  /** constant value for the bllimit-parameter. **/
  private static final int LIMIT = 50;

  /** object creating the requests that are sent to the api. */
  private RequestCreator requestBuilder = null;

  private final String articleName;

  private final MediaWikiBot bot;
  private final RedirectFilter redirectFilter;
  private final int[] namespaces;

  /**
   * The public constructor. It will have a MediaWiki-request generated, which is then added to
   * msgs. When it is answered, the method processAllReturningText will be called (from outside this
   * class).
   * 
   * @param articleName
   *          the title of the article, != null
   * @param namespaces
   *          the namespace(s) that will be searched for links, as a string of numbers separated by
   *          '|'; if null, this parameter is omitted. See for e.g. {@link MediaWiki#NS_ALL}.
   * @param redirectFilter
   *          filter that determines how to handle redirects, must be all for MW versions before
   *          1.11; != null
   */
  public BacklinkTitles(MediaWikiBot bot, String articleName, RedirectFilter redirectFilter,
      int... namespaces) {
    super(bot);
    assert bot != null;
    assert articleName != null && redirectFilter != null;
    this.redirectFilter = redirectFilter;
    this.namespaces = namespaces;

    this.articleName = articleName;
    this.bot = bot;
    requestBuilder = createRequestBuilder(bot.getVersion());

  }

  public BacklinkTitles(MediaWikiBot bot, String articleName) {
    this(bot, articleName, RedirectFilter.all, null);

  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    return new BacklinkTitles(bot, articleName, redirectFilter, namespaces);
  }

  /**
   * gets the information about a follow-up page from a provided api response. If there is one, the
   * information for the next page parameter is added to the nextPageInfo field.
   * 
   * @param s
   *          text for parsing
   */
  @Override
  protected String parseHasMore(final String s) {
    log.trace(s);
    // TODO do not use pattern matching
    // get the blcontinue-value
    Pattern p = Pattern.compile("<query-continue>.*?" + "<backlinks *blcontinue=\"([^\"]*)\" */>"
        + ".*?</query-continue>", Pattern.DOTALL | Pattern.MULTILINE);

    Matcher m = p.matcher(s);

    if (m.find()) {
      return m.group(1);
    } else {
      return "";
    }

  }

  /**
   * picks the article name from a MediaWiki api response.
   * 
   * @param s
   *          text for parsing
   */
  @Override
  protected Collection<String> parseArticleTitles(String s) {

    // get the other backlink titles and add them all to the titleCollection
    Collection<String> titleCollection = Lists.newArrayList();

    Pattern p = Pattern
        .compile("<bl pageid=\".*?\" ns=\".*?\" title=\"([^\"]*)\" (redirect=\"\" )?/>");

    Matcher m = p.matcher(s);

    while (m.find()) {
      titleCollection.add(m.group(1));

    }
    return titleCollection;

  }

  /**
   * creates a request builder for the given API version.
   * 
   * @param apiVersion
   *          for which the request builder is working.
   */
  private RequestCreator createRequestBuilder(Version apiVersion) {

    switch (apiVersion) {

    case MW1_15:
    case MW1_16:
      return new RequestCreator1x15();

    default: // MW1_17 and up
      return new RequestCreator1x17();

    }

  }

  /** interface for classes that create a request strings. */
  private interface RequestCreator {

    /**
     * generates an initial MediaWiki-request.
     * 
     * @return the request in string form
     */
    Get newInitialRequest(String articleName, RedirectFilter redirectFilter, int[] namespace);

    /**
     * generates a follow-up MediaWiki-request.
     * 
     * @param blcontinue
     *          key for continuing
     * @return the request in string form
     */
    Get newContinueRequest(String articleName, String blcontinue);

  }

  private static RequestBuilder newRequestBuilder() {
    return new ApiRequestBuilder() //
        .action("query") //
        .formatXml() //
        .param("list", "backlinks") //
        .param("bllimit", LIMIT + "") //
    ;
  }

  /** request builder for MW versions 1_17 onwards. */
  private static class RequestCreator1x17 implements RequestCreator {
    /**
     * {@inheritDoc}
     */
    public Get newInitialRequest(String articleName, RedirectFilter redirectFilter, int[] namespace) {
      RequestBuilder requestBuilder = newRequestBuilder() //
          .param("bltitle", MediaWiki.encode(articleName)) //
          .param("blfilterredir", MediaWiki.encode(redirectFilter.toString())) //
      ;
      if (namespace != null) {
        requestBuilder.param("blnamespace", MediaWiki.encode(MWAction.createNsString(namespace)));
      }
      return requestBuilder.buildGet();
    }

    /**
     * {@inheritDoc}
     */
    public Get newContinueRequest(String articleName, String blcontinue) {
      return newRequestBuilder() //
          .param("blcontinue", MediaWiki.encode(blcontinue)) //
          .param("bltitle", MediaWiki.encode(articleName)) //
          .buildGet();
    }

  }

  /** request builder for MW versions 1_11 to (at least) 1_13. */
  private static class RequestCreator1x15 implements RequestCreator {
    /**
     * {@inheritDoc}
     */
    public Get newInitialRequest(String articleName, RedirectFilter redirectFilter, int[] namespace) {
      RequestBuilder requestBuilder = newRequestBuilder() //
          .param("bltitle", MediaWiki.encode(articleName)) //
          .param("blfilterredir", MediaWiki.encode(redirectFilter.toString())) //
      ;
      if (namespace != null) {
        requestBuilder.param("blnamespace", MediaWiki.encode(MWAction.createNsString(namespace)));
      }
      return requestBuilder.buildGet();
    }

    /**
     * {@inheritDoc}
     */
    public Get newContinueRequest(String articleName, String blcontinue) {
      return newRequestBuilder() //
          .param("blcontinue", MediaWiki.encode(blcontinue)) //
          .buildGet();
    }

  }

  @Override
  protected HttpAction prepareCollection() {
    if (getNextPageInfo().length() > 0) {
      return requestBuilder.newContinueRequest(articleName, getNextPageInfo());
    } else {
      return requestBuilder.newInitialRequest(articleName, redirectFilter, namespaces);
    }
  }

}
