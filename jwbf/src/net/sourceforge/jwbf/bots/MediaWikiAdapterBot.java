/*
 * Copyright 2007 Thomas Stock.
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
 * Justus Bisser - thanks for file upload methods
 */

package net.sourceforge.jwbf.bots;


import static net.sourceforge.jwbf.actions.mediawiki.MediaWiki.NS_MAIN;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;

import net.sourceforge.jwbf.actions.mediawiki.editing.FileUpload;
import net.sourceforge.jwbf.actions.mediawiki.editing.PostDelete;
import net.sourceforge.jwbf.actions.mediawiki.queries.GetAllPageTitles;
import net.sourceforge.jwbf.actions.mediawiki.queries.GetBacklinkTitles;
import net.sourceforge.jwbf.actions.mediawiki.queries.GetFullCategoryMembers;
import net.sourceforge.jwbf.actions.mediawiki.queries.GetImageInfo;
import net.sourceforge.jwbf.actions.mediawiki.queries.GetImagelinkTitles;
import net.sourceforge.jwbf.actions.mediawiki.queries.GetLogEvents;
import net.sourceforge.jwbf.actions.mediawiki.queries.GetRecentchanges;
import net.sourceforge.jwbf.actions.mediawiki.queries.GetSimpleCategoryMembers;
import net.sourceforge.jwbf.actions.mediawiki.queries.GetTemplateUserTitles;
import net.sourceforge.jwbf.actions.mediawiki.queries.GetBacklinkTitles.RedirectFilter;
import net.sourceforge.jwbf.actions.util.ActionException;
import net.sourceforge.jwbf.actions.util.ProcessException;
import net.sourceforge.jwbf.contentRep.ContentAccessable;
import net.sourceforge.jwbf.contentRep.mediawiki.CategoryItem;
import net.sourceforge.jwbf.contentRep.mediawiki.LogItem;
import net.sourceforge.jwbf.contentRep.mediawiki.SimpleFile;

/**
 * 
 * This class helps you to interact with each 
 * <a href="http://www.mediawiki.org" target="_blank">MediaWiki</a>. This class offers
 * a set of methods which are defined in the package net.sourceforge.jwbf.actions.mw.*
 * 
 * If you need more options, use these classes directly.
 *
 * How to use:
 *
 * <pre>
 * MediaWikiBot b = new MediaWikiBot(&quot;http://yourwiki.org&quot;);
 * b.login(&quot;Username&quot;, &quot;Password&quot;);
 * System.out.writeln(b.readContentOf(&quot;Main Page&quot;).getText());
 * </pre>
 *
 * @author Thomas Stock
 * @author Tobias Knerr
 * @author Justus Bisser
 * 
 */
public class MediaWikiAdapterBot extends MediaWikiBot {


	/**
	 * Design only for extension.
	 */
	protected MediaWikiAdapterBot() {
		// do nothing, design only for extension
	}
	
	/**
	 * @param u
	 *            wikihosturl like "http://www.mediawiki.org/wiki/"
	 */
	public MediaWikiAdapterBot(final URL u) {
		super(u);

	}

	/**
	 * @param url
	 *            wikihosturl like "http://www.mediawiki.org/wiki/"
	 * @throws MalformedURLException
	 *            if param url does not represent a well-formed url
	 */
	public MediaWikiAdapterBot(final String url) throws MalformedURLException {
		super(url);

	}
	
	/**
	 * @param url
	 *            wikihosturl like "http://www.mediawiki.org/wiki/"
	 * @param testHostReachable if true, test if host reachable
	 * @throws IOException a
	 * @throws UnknownHostException a
	 */
	public MediaWikiAdapterBot(final URL url, boolean testHostReachable) throws UnknownHostException, IOException {
		super(url, testHostReachable);

	}


	/**
	 * @see GetAllPageTitles
	 */
	public Iterable<String> getAllPageTitles(String from, String prefix,
			boolean redirects, boolean nonredirects, int... namespaces)
			throws ActionException {
		GetAllPageTitles a = new GetAllPageTitles( from, prefix, redirects, nonredirects, this, namespaces );
		return a;
	}

	/**
	 * @see GetAllPageTitles
	 */
	public Iterable<String> getAllPageTitles(int... namespaces)
			throws ActionException {
		return getAllPageTitles(null, null, false, true, namespaces);


	}

	/**
	 * @see GetAllPageTitles
	 */
	public Iterable<String> getAllPageTitles(String from, String prefix,
			boolean redirects, boolean nonredirects) throws ActionException {

		return getAllPageTitles(from, prefix, redirects, nonredirects, null);

	}

	/**
	 * @see GetBacklinkTitles
	 */
	public Iterable<String> getBacklinkTitles(String article,
			RedirectFilter redirectFilter, int... namespaces)
			throws ActionException, ProcessException {

		GetBacklinkTitles a = new GetBacklinkTitles(article,
				redirectFilter, this, namespaces);

		return a;
	}

	/**
	 * @see GetBacklinkTitles
	 */
	public Iterable<String> getBacklinkTitles(String article, int... namespaces)
			throws ActionException, ProcessException {

		return getBacklinkTitles(article, RedirectFilter.all, namespaces);
	}
	/**
	 * @see GetBacklinkTitles
	 */
	public Iterable<String> getBacklinkTitles(String article, RedirectFilter redirectFilter)
			throws ActionException, ProcessException {

		return getBacklinkTitles(article, redirectFilter, null);
	}


	/**
	 *  @see GetBacklinkTitles
	 */
	public Iterable<String> getBacklinkTitles(String article)
			throws ActionException, ProcessException {

		return getBacklinkTitles( article, RedirectFilter.all, null);
	}


	/**
	 * @see PostDelete
	 */
	public void postDelete(String title) throws ActionException, ProcessException {
		
		performAction(new PostDelete(title, getSiteinfo(), getUserinfo()));
	}

	/**
	 *
	 * @see GetSimpleCategoryMembers
	 */
	public Iterable<String> getCategoryMembers(String category) throws ActionException, ProcessException {
		return getCategoryMembers(category, NS_MAIN);
	}

	/**
	 * @see GetSimpleCategoryMembers
	 */
	public Iterable<String> getCategoryMembers(String category, int... namespaces) throws ActionException, ProcessException {
		GetSimpleCategoryMembers c = new GetSimpleCategoryMembers(category, this, namespaces);
		return c;

	}
	/**
	 *
	 * @see GetFullCategoryMembers
	 */
	public Iterable<CategoryItem> getFullCategoryMembers(String category) throws ActionException, ProcessException {
		return getFullCategoryMembers(category, NS_MAIN);
	}
	/**
	 *
	 * @see GetFullCategoryMembers
	 */
	public Iterable<CategoryItem> getFullCategoryMembers(String category, int... namespaces) throws ActionException, ProcessException {
		GetFullCategoryMembers c = new GetFullCategoryMembers(category, this, namespaces );
		return c;
		
	}

	/**
	 * @param fileName a
	 * @see FileUpload
	 * TODO exception missing
	 */
	public final void uploadFile(final String fileName) throws ActionException,
			ProcessException {

		File f = new File(fileName);

		SimpleFile a = new SimpleFile(f.getName(), fileName);
		performAction(new FileUpload(a, this));
		
		

	}

	/**
	 * @param file a
	 * @see FileUpload 
	 * TODO exception missing
	 */
	public void uploadFile(SimpleFile file) throws ActionException,
			ProcessException {
		performAction(new FileUpload(file, this));
	}

	/**
	 * get the titles of all pages which contain a link to the given image.
	 *
	 * @param image
	 *            title of an image, without prefix "Image:"
	 *
	 * @param namespaces
	 *            numbers of the namespaces (specified using varargs) that will
	 *            be included in the search
	 *
	 * @return iterable providing access to the names of all articles which link
	 *         to the image specified by the image-parameter. Attention: to get
	 *         more article titles, the connection to the MediaWiki must still
	 *         exist.
	 *
	 * @throws ActionException
	 *             on problems with http, cookies and io
	 *
	 *
	 * @see GetImagelinkTitles
	 *
	 */
	public Iterable<String> getImagelinkTitles(String image, int... namespaces)
			throws ActionException {
		GetImagelinkTitles a = new GetImagelinkTitles(image,
				namespaces);

		return a;

	}

	/**
	 * variation of the getImagelinkTitles-method which does not set a namespace
	 * restriction.
	 *
	 * @param image name of
	 * @return an of labels
	 * @see #getImagelinkTitles(String, int[])
	 * @throws ActionException
	 *             on problems with http, cookies and io
	 * @see #getImagelinkTitles(String, int...)
	 */
	public Iterable<String> getImagelinkTitles(String image)
			throws ActionException {

		return getImagelinkTitles(image, null);

	}
	/**
	 * @param imagename a
	 * @see GetImageInfo
	 */
	public String getImageInfo(String imagename) throws ActionException, ProcessException {
		
		GetImageInfo a = new GetImageInfo(imagename, getVersion(), getHostUrl());
		performAction(a);
		return a.getUrlAsString();
	}

	/**
	 *
	 * @param type event type like: upload, delete, ...
	 * @return the last ten log events
	 * @throws ActionException on problems with http, cookies and io
	 * @throws ProcessException 
	 * @supportedBy MediaWikiAPI 1.11 logevents / le
	 * @see #getLogEvents(int, String...)
	 */
	public Collection<LogItem> getLogEvents(String type) throws ActionException, ProcessException {

		return getLogEvents(10, type);
	}

	/**
	 *
	 * @param type event type like: upload, delete, ...
	 * @param limit number of events
	 * @return the last ten log events
	 * @throws ActionException on problems with http, cookies and io
	 * @throws ProcessException 
	 * @supportedBy MediaWikiAPI 1.11 logevents / le TODO Test Required
	 * TODO API state is (semi-complete), see
	 * http://www.mediawiki.org/wiki/API:Query_-_Lists#logevents_.2F_le_.28semi-complete.29
	 */
	public Collection<LogItem> getLogEvents(int limit, String type) throws ActionException, ProcessException {
		GetLogEvents c = new GetLogEvents(limit, type, this);
		
		performAction(c);
		return c.getResults();
		
		
	}
	/**
	 * get the titles of all pages which embed the given template.
	 *
	 * @param template
	 *            title of a template, without prefix "Template:"
	 *
	 * @param namespaces
	 *            numbers of the namespaces (specified using varargs) that will
	 *            be included in the search
	 *
	 * @return iterable providing access to the names of all articles which
	 *         embed the template specified by the template-parameter.
	 *         Attention: to get more article titles, the connection to the
	 *         MediaWiki must still exist.
	 *
	 * @throws ActionException
	 *             on problems with http, cookies and io
	 *
	 */
	public Iterable<String> getTemplateUserTitles(String template,
			int... namespaces) throws ActionException {
		
		GetTemplateUserTitles a = new GetTemplateUserTitles(template,
				namespaces);

		return a;

	}

	/**
	 * variation of the getTemplateUserTitles-method. which does not set a
	 * namespace restriction
	 *
	 * @param template
	 *            label of template like TODO what ?, without prefix "Template:"
	 * @return an of labels
	 * @throws ActionException
	 *             on problems with http, cookies and io
	 * @see #getTemplateUserTitles(String, int...)
	 */
	public Iterable<String> getTemplateUserTitles(String template)
			throws ActionException {
		return getTemplateUserTitles(template, null);

	}



	/**
	 * Get a number of recent changes from namespace.
	 *
	 * @param count
	 *            of changes
	 * @param namespaces
	 *            namespacenumbers greater equals 0
	 * @return a
	 * @throws ActionException
	 *             on problems with http, cookies and io
	 * @see GetRecentchanges
	 */
	public Iterable<String> getRecentchangesTitles(
			int... namespaces) throws ActionException {
		GetRecentchanges a = new GetRecentchanges(this,
				namespaces);
		
		return a;
		
	}

	/**
	 * Get a number of recent changes from default namespace.
	 *
	 * @param count
	 *            of changes
	 * @return a
	 * @throws ActionException
	 *             on problems with http, cookies and io
	 * @supportedBy MediaWikiAPI 1.10 recentchanges / rc
	 * TODO TEST
	 */
	public Iterable<String> getRecentchangesTitles()
			throws ActionException {

		return getRecentchangesTitles(NS_MAIN);
	}



	/**
	 * Writes an iteration of ContentAccessables to the mediawiki.
	 *
	 * @param cav
	 *            a
	 * @throws ActionException
	 *             on problems with http, cookies and io
	 * @throws ProcessException on acces problems
	 * @supportedBy MediaWiki 1.9.x, 1.10.x, 1.11.x, 1.12.x, 1.13.x, 1.14.x
	 */
	public final void writeMultContent(final Iterator<ContentAccessable> cav)
			throws ActionException, ProcessException {
		while (cav.hasNext()) {
			writeContent(cav.next());

		}
	}

}