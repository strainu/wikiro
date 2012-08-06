package info.astroe.was;

import static org.wikipedia.Wiki.CATEGORY_TALK_NAMESPACE;
import static org.wikipedia.Wiki.HIDE_BOT;
import static org.wikipedia.Wiki.HIDE_SELF;
import static org.wikipedia.Wiki.PROJECT_NAMESPACE;
import static org.wikipedia.Wiki.PROJECT_TALK_NAMESPACE;
import static org.wikipedia.Wiki.TALK_NAMESPACE;
import static org.wikipedia.Wiki.USER_TALK_NAMESPACE;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.login.FailedLoginException;

import org.wikipedia.Wiki;
import org.wikipedia.Wiki.Revision;

public class AutoSigner {

	private static Wiki wiki;

	private static Set<String> projectPages = new HashSet<String>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = -2245300967753508562L;

		{
			add("Wikipedia:Cafenea");
		}
	};

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		wiki = new Wiki("ro.wikipedia.org");
		Properties credentials = new Properties();

		try {
			credentials.load(AutoSigner.class.getClassLoader()
					.getResourceAsStream("credentials.properties"));

			String username = credentials.getProperty("Username");
			String password = credentials.getProperty("Password");
			wiki.login(username, password.toCharArray());
			Revision[] revisions = wiki.recentChanges(20, HIDE_BOT | HIDE_SELF,
					new int[] { TALK_NAMESPACE, USER_TALK_NAMESPACE,
							PROJECT_TALK_NAMESPACE, PROJECT_NAMESPACE,
							CATEGORY_TALK_NAMESPACE });
			revisions: for (Revision rev : revisions) {
				System.out.println(rev.getPage() + "\t" + rev.getRevid());
				if (rev.getRevid() == 0) {
					continue revisions;
				}
				if (rev.getPage().startsWith("Wikipedia:")
						&& !projectPages.contains(rev.getPage())) {
					continue revisions;
				}
				String crtText = rev.getText();
				String crtAuthor = rev.getUser();

				// get a chunk of history large enough to compare
				Calendar now = Calendar.getInstance();
				Calendar yesterday = Calendar.getInstance();
				yesterday.roll(Calendar.DATE, false);
				Revision[] pageHistory = wiki.getPageHistory(rev.getPage(),
						now, yesterday);
				while (pageHistory.length < 2 && countEditors(pageHistory) < 2
						&& !isNew(pageHistory[pageHistory.length - 1])) {
					yesterday.add(Calendar.MONTH, -1);
					pageHistory = wiki.getPageHistory(rev.getPage(), now,
							yesterday);
				}

				if (isNew(pageHistory[0])) {
					analyzeText(pageHistory[0]);
					continue revisions;
				}
				// locate the last message entered by another user
				int i;
				Revision lastMessageByOther = pageHistory[0];
				for (i = 1; i < pageHistory.length; i++) {
					if (isNew(pageHistory[i])) {
						analyzeText(pageHistory[0]);
						continue revisions;
					}
					// for edits spaced larger than 1 hour, we consider them
					// different messages
					long timediff = pageHistory[i].getTimestamp()
							.getTimeInMillis()
							- pageHistory[0].getTimestamp().getTimeInMillis();
					if (!pageHistory[i].getUser().equals(crtAuthor)
							|| timediff > 1000 * 60 * 60) {
						lastMessageByOther = pageHistory[i];
						break;
					}
				}
				if (lastMessageByOther.getRevid() == rev.getRevid()) {
					analyzeText(rev);
					continue revisions;
				}
				String diff = rev.diff(lastMessageByOther);
				analyzeDiff(diff);
			}
		} catch (FailedLoginException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void analyzeDiff(String diff) {
		// TODO Auto-generated method stub
		System.out.println(diff);
	}

	private static void analyzeText(Revision revision) throws IOException {
		String text = revision.getText();
		if (isTemplatesOnly(text)) {
			System.out.println("Page made up only of templates");
			return;
		}
		System.out.println(text);
		Pattern userPageDetector = Pattern
				.compile("\\[\\[\\:?((Utilizator\\:)|(User\\:))"
						+ revision.getUser());
		Pattern userTalkPageDetector = Pattern
				.compile("\\[\\[\\:?((Discu\u021Bie Utilizator\\:)|(User talk\\:))"
						+ revision.getUser());
		Matcher matcher = userPageDetector.matcher(text);
		if (matcher.find()) {
			System.out.println("Mesaj de " + revision.getUser()
					+ " semnat cu link spre pagina de utilizator.");
		}
		matcher = userTalkPageDetector.matcher(text);
		if (matcher.find()) {
			System.out.println("Mesaj de " + revision.getUser()
					+ " semnat cu link spre pagina de discuţii utilizator.");
		}

		StringBuilder textToAdd = new StringBuilder("{{subst:nesemnat|");
		textToAdd.append(revision.getUser());
		textToAdd.append("|data=");
		Calendar timestamp = revision.getTimestamp();
		SimpleDateFormat dateFormatter = new SimpleDateFormat("d MMMM yyyy HH:mm", new Locale("ro"));
		textToAdd.append(dateFormatter.format(timestamp.getTime()));
		textToAdd.append("}}");
		
		//TODO: actually append the template
		System.out.println("I should append " + textToAdd);
	}

	private static boolean isTemplatesOnly(String text) {
		Pattern templatesDetectorPattern = Pattern
				.compile("\\s*(\\{\\{.*\\}\\}\\s*)+");
		Matcher matcher = templatesDetectorPattern.matcher(text);
		return matcher.matches();
	}

	private static int countEditors(Revision[] pageHistory) {
		Set<String> editors = new HashSet<String>();
		for (Revision rev : pageHistory) {
			editors.add(rev.getUser());
		}
		return editors.size();
	}

	private static boolean isNew(Revision rev) throws IOException {
		if (rev.isNew()) {
			return true;
		}

		String page = rev.getPage();
		Revision firstPageRev = wiki.getFirstRevision(page);
		return firstPageRev.getRevid() == rev.getRevid();
	}
}