package net.sourceforge.jwbf.mediawiki.live.auto;

import static net.sourceforge.jwbf.TestHelper.getRandom;
import static net.sourceforge.jwbf.mediawiki.LiveTestFather.getSpecialChars;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.jwbf.core.actions.util.ActionException;
import net.sourceforge.jwbf.core.contentRep.Article;
import net.sourceforge.jwbf.core.contentRep.SimpleArticle;
import net.sourceforge.jwbf.mediawiki.VersionTestClassVerifier;
import net.sourceforge.jwbf.mediawiki.actions.MediaWiki.Version;
import net.sourceforge.jwbf.mediawiki.actions.queries.RecentchangeTitles;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Verifier;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

@Slf4j
public class RecentChangesTest extends ParamHelper {
  private static final int COUNT = 3;
  private static final int LIMIT = COUNT * 2;

  @ClassRule
  public static VersionTestClassVerifier classVerifier = new VersionTestClassVerifier(
      RecentchangeTitles.class);

  @Rule
  public Verifier successRegister = classVerifier.getSuccessRegister(this);

  private Stopwatch watch;

  @Parameters(name = "{0}")
  public static Collection<?> stableWikis() {
    return ParamHelper.prepare(Version.valuesStable());
  }

  public RecentChangesTest(Version v) {
    super(v, classVerifier);
  }

  @Before
  public void before() {
    watch = new Stopwatch();
    watch.start();
  }

  @After
  public void after() {
    watch.stop();
  }

  @Test
  public final void recentChanges() {
    doRegularTest(bot);
    doSpecialCharTest(bot);
  }

  private void prepareWiki(MediaWikiBot bot) {
    log.debug("begin prepare " + watch.elapsed(TimeUnit.MILLISECONDS));
    SimpleArticle a = new SimpleArticle();
    for (int i = 0; i < 2 + 1; i++) {
      String label = getRandom(10);
      for (char c : MediaWikiBot.INVALID_LABEL_CHARS) {
        label = label.replace(c + "", "");
      }
      a.setTitle(label);
      a.setText(getRandom(23));
      bot.writeContent(a);
    }
    log.debug("end prepare " + watch.elapsed(TimeUnit.MILLISECONDS));

  }

  private void doSpecialCharTest(MediaWikiBot bot) {
    Article sa;
    String testText = getRandom(255);

    Collection<String> specialChars = getSpecialChars();
    try {
      for (String title : specialChars) {
        sa = new Article(bot, title);
        sa.setText(testText);
        sa.save();
      }
    } catch (ActionException e) {
      boolean found = false;
      for (char ch : MediaWikiBot.INVALID_LABEL_CHARS) {
        if (e.getMessage().contains(ch + "")) {
          found = true;
          break;
        }
      }
      assertTrue("should be a know invalid char", found);
    }

    RecentchangeTitles rc = new RecentchangeTitles(bot);

    Iterator<String> is = rc.iterator();
    int i = 0;
    int size = specialChars.size();

    while (is.hasNext() && i < (size * 1.2)) {
      String specialChar = is.next();
      log.debug("rm " + specialChar);
      specialChars.remove(specialChar);
      i++;
    }
    for (char c : MediaWikiBot.INVALID_LABEL_CHARS) {
      specialChars.remove(c + "");
    }

    assertTrue("tc sould be empty but is: " + specialChars, specialChars.isEmpty());

  }

  private void doRegularTest(MediaWikiBot bot) {
    prepareWiki(bot);
    RecentchangeTitles rc = new RecentchangeTitles(bot);

    int i = 0;
    Iterator<String> titles = rc.iterator();
    List<Integer> vi = fillExpectedElements();
    try {
      i = check(i, titles, vi, true);
      if (!vi.isEmpty()) {
        throw new Exception();
      }
    } catch (Exception e) {
      change(bot);
      titles = rc.iterator();
      i = 0;
      vi = fillExpectedElements();
      i = check(i, titles, vi, false);
    }
    assertTrue("shuld be empty but is : " + vi, vi.isEmpty());
    assertTrue("i is: " + i, i > COUNT - 1);
    if (false) {
      // TODO use api or better filter
      rc = new RecentchangeTitles(bot, true);
      titles = rc.iterator();
      List<String> uniqTitle = Lists.newArrayList();
      for (int j = 0; j <= COUNT; j++) {
        uniqTitle.add(titles.next());
      }
      assertEquals("", Joiner.on("\n").join(uniqTitle));
    }
  }

  private final Pattern changePattern = Pattern.compile("%Change ([0-9]+)");

  protected int check(int i, Iterator<String> titles, List<Integer> vi, boolean failable) {
    while (titles.hasNext()) {
      String title = titles.next();
      log.debug(title);
      Matcher titleMatcher = changePattern.matcher(title);

      if (titleMatcher.matches()) {
        int x = Integer.parseInt(titleMatcher.group(1));
        vi.remove(Integer.valueOf(x));
      } else {
        if (failable) {
          throw new IllegalStateException();
        }
      }
      i++;
      if (i > LIMIT || vi.isEmpty()) {
        break;
      }
    }
    return i;
  }

  private List<Integer> fillExpectedElements() {
    List<Integer> vi = Lists.newArrayList();
    for (int j = 0; j < COUNT; j++) {
      if (j == 0) {
        j = 1;
      }
      vi.add(Integer.valueOf(j));
    }
    return vi;
  }

  private void change(MediaWikiBot bot) {
    SimpleArticle a = new SimpleArticle();
    for (int i = 0; i < COUNT + 1; i++) {
      int j;
      if (i == 0) {
        j = 1;
      } else {
        j = i;
      }
      a.setTitle("%Change " + j);
      a.setText(getRandom(255));
      bot.writeContent(a);
    }
  }
}
