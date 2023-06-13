package org.ezcampus.search.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ezcampus.search.System.GlobalSettings;
import org.ezcampus.search.System.ResourceLoader;
import org.ezcampus.search.core.models.response.CourseDataResult;
import org.ezcampus.search.data.StringHelper;
import org.ezcampus.search.hibernate.entity.Course;
import org.ezcampus.search.hibernate.entity.CourseData;
import org.ezcampus.search.hibernate.entity.CourseFaculty;
import org.ezcampus.search.hibernate.entity.Meeting;
import org.ezcampus.search.hibernate.entity.Word;
import org.ezcampus.search.hibernate.entity.WordMap;
import org.ezcampus.search.hibernate.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.tinylog.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class SearchHandler
{
	public static void main(String[] args)
	{
		GlobalSettings.IS_DEBUG = true;
		ResourceLoader.loadTinyLogConfig();

		String term = "calculus II";

		Logger.info("");

//		List<CourseDataResult> results1 = searchExactWords(term, 1, 5, 202305);
		long start = System.nanoTime();
		List<CourseDataResult> results2 = searchFuzzy(term, 1, 5, 202305);
		double elapsedTime = (System.nanoTime() - start) / 1_000_000_000.0; // Convert nanoseconds to seconds
		Logger.info("Finished searchFuzzy1 after {} seconds", elapsedTime);

		start = System.nanoTime();
		List<CourseDataResult> results3 = searchFuzzy2(term, 1, 5, 202305);
		elapsedTime = (System.nanoTime() - start) / 1_000_000_000.0; // Convert nanoseconds to seconds
		Logger.info("Finished searchFuzzy2 after {} seconds", elapsedTime);
		
		results2.forEach(x -> {
			Logger.info(x);
		});
		Logger.info("");
		results3.forEach(x -> {
			Logger.info(x);
		});

	}

	public static List<CourseDataResult> loadIn(List<CourseData> results, Session session)
	{
		ArrayList<CourseDataResult> cdr = new ArrayList<>(results.size());

		if (results.size() == 0)
			return cdr;

		for (CourseData courseData : results)
		{
			Course course = courseData.getCourse();

			List<CourseFaculty> facultyQuery = session.createQuery(
					"SELECT cf FROM CourseFaculty cf WHERE cf.courseDataId = :courseData", CourseFaculty.class
			).setParameter("courseData", courseData).getResultList();

			List<Meeting> meetingQuery = session
					.createQuery("SELECT m FROM Meeting m WHERE m.courseDataId = :courseData", Meeting.class)
					.setParameter("courseData", courseData).getResultList();

			CourseDataResult combinedEntry = new CourseDataResult(course, courseData, facultyQuery, meetingQuery);
			
			
			
//			String query = "SELECT cf, m FROM CourseFaculty cf JOIN Meeting m " +
//			        "WHERE cf.courseDataId = :courseData AND m.courseDataId = :courseData";
//
//			List<Object[]> resultList = session.createQuery(query, Object[].class)
//			        .setParameter("courseData", courseData)
//			        .getResultList();
//
//			CourseDataResult combinedEntry = new CourseDataResult(course, courseData, resultList);
			

			cdr.add(combinedEntry);
		}

		return cdr;
	}

	public static void rank(List<WordMap> matchingEntries, List<CourseData> relevantCDs)
	{
		for (WordMap wordMap : matchingEntries)
		{
			CourseData courseData = wordMap.getCourseData();

			boolean isNewEntry = true;

			for (CourseData relevantCD : relevantCDs)
			{
				if (relevantCD.equals(courseData))
				{
					relevantCD.ranking += wordMap.getCount();
					isNewEntry = false;
					break;
				}
			}

			if (isNewEntry)
			{
				relevantCDs.add(courseData);
			}
		}
	}

	
	public static List<CourseDataResult> searchFuzzy2(String searchTerm, int page, int resultsPerPage, int termId)
	{
		// Look up in the word index
		ArrayList<CourseData> relevantCDs = new ArrayList<>();

		if (searchTerm == null)
			return new ArrayList<>();

		// Calculate the offset based on the page number
		int pageoffset = resultsPerPage * (page - 1);

		try (Session session = HibernateUtil.getSessionFactory().openSession())
		{
			Arrays.stream(searchTerm.split("\\s+"))
				.map(StringHelper::cleanWord)
				.filter(word -> !word.isEmpty())
				.forEach(word -> 
				{
					String query = "SELECT wm FROM WordMap wm " +
					        "JOIN wm.word w " +
					        "JOIN wm.courseData cd " +
					        "JOIN cd.course c " +
					        "JOIN c.term t " +
					        "WHERE CONCAT('%', w.word, '%') LIKE :targetWord " +
					        "AND t.termId = :termId";
					
					List<WordMap> matchingEntries = session.createQuery(query, WordMap.class)
					        .setParameter("targetWord", "%" + word + "%")
					        .setParameter("termId", termId)
					        .getResultList();
					
					rank(matchingEntries, relevantCDs);
//					
//						List<Word> matchingWordList = session
//								.createQuery("FROM Word w WHERE CONCAT('%', w.word, '%') LIKE :targetWord", Word.class)
//								.setParameter("targetWord", "%" + word + "%").getResultList();
//
//						for (Word matchingWord : matchingWordList)
//						{
//							Logger.debug("Found WORD: {} ID: {}", matchingWord.getWordString(), matchingWord.getId());
//
//							List<WordMap> matchingEntries = session
//									.createQuery(
//											"FROM WordMap wm WHERE wm.word = :targetId "
//													+ "AND wm.courseData.course.term.termId = :termId",
//											WordMap.class
//									).setParameter("targetId", matchingWord).setParameter("termId", termId)
//									// .setFirstResult(pageoffset)
//									// .setMaxResults(resultsPerPage)
//									.list();
//
//							rank(matchingEntries, relevantCDs);
//						}
					});

			return loadIn(relevantCDs, session);
		}
	}
	
	
	public static List<CourseDataResult> searchFuzzy(String searchTerm, int page, int resultsPerPage, int termId)
	{
		// Look up in the word index
		ArrayList<CourseData> relevantCDs = new ArrayList<>();

		if (searchTerm == null)
			return new ArrayList<>();

		// Calculate the offset based on the page number
		int pageoffset = resultsPerPage * (page - 1);

		try (Session session = HibernateUtil.getSessionFactory().openSession())
		{
			Arrays.stream(searchTerm.split("\\s+")).map(StringHelper::cleanWord).filter(word -> !word.isEmpty())
					.forEach(word -> {
						List<Word> matchingWordList = session
								.createQuery("FROM Word w WHERE CONCAT('%', w.word, '%') LIKE :targetWord", Word.class)
								.setParameter("targetWord", "%" + word + "%").getResultList();

						for (Word matchingWord : matchingWordList)
						{
							Logger.debug("Found WORD: {} ID: {}", matchingWord.getWordString(), matchingWord.getId());

							List<WordMap> matchingEntries = session
									.createQuery(
											"FROM WordMap wm WHERE wm.word = :targetId "
													+ "AND wm.courseData.course.term.termId = :termId",
											WordMap.class
									).setParameter("targetId", matchingWord).setParameter("termId", termId)
									// .setFirstResult(pageoffset)
									// .setMaxResults(resultsPerPage)
									.list();

							rank(matchingEntries, relevantCDs);
						}
					});

			return loadIn(relevantCDs, session);
		}
	}

	public static List<CourseDataResult> searchExactWords(String searchTerm, int page, int resultsPerPage, int termId)
	{
		// Look up in the word index
		ArrayList<CourseData> relevantCDs = new ArrayList<>();

		if (searchTerm == null)
			return new ArrayList<>();

		// Calculate the offset based on the page number
		int pageoffset = resultsPerPage * (page - 1);

		try (Session session = HibernateUtil.getSessionFactory().openSession())
		{
			Arrays.stream(searchTerm.split("\\s+")).map(StringHelper::cleanWord).filter(word -> !word.isEmpty())
					.forEach(word -> {
						Word matchingWord = session.createQuery("FROM Word w WHERE w.word = :targetWord", Word.class)
								.setParameter("targetWord", word).uniqueResult();

						if (matchingWord == null)
						{
							return;
						}

						Logger.debug("WORD: {} ID: {}", matchingWord.getWordString(), matchingWord.getId());

						List<WordMap> matchingEntries = session
								.createQuery(
										"FROM WordMap wm WHERE wm.word = :targetId "
												+ "AND wm.courseData.course.term.termId = :termId",
										WordMap.class
								).setParameter("targetId", matchingWord).setParameter("termId", termId)
								.setFirstResult(pageoffset).setMaxResults(resultsPerPage).list();

						rank(matchingEntries, relevantCDs);
					});

			return loadIn(relevantCDs, session);
		}
	}

}
