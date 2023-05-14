package org.ezcampus.search.data;

import java.util.List;

import org.ezcampus.search.System.ResourceLoader;
import org.ezcampus.search.hibernate.entity.CourseData;
import org.ezcampus.search.hibernate.entity.CourseFaculty;
import org.ezcampus.search.hibernate.entity.Faculty;
import org.ezcampus.search.hibernate.entity.Meeting;
import org.ezcampus.search.hibernate.entity.ScrapeHistory;
import org.ezcampus.search.hibernate.entity.ScrapeHistoryDAO;
import org.ezcampus.search.hibernate.entity.WordDAO;
import org.ezcampus.search.hibernate.util.HibernateUtil;
import org.hibernate.Session;
import org.tinylog.Logger;

public class DatabaseProcessing
{
	public static final String WORD_INSERT_SPLIT_REGEX = "[\\s,_]";

	public static void main(String[] args)
	{
		ResourceLoader.loadTinyLogConfig();
		processLastScrape();
	}

	public static void splitInsertWord(String value, CourseData courseData, Session session)
	{
		if(value == null) return;
		
		for (String v : value.split(WORD_INSERT_SPLIT_REGEX))
		{
			if (v == null || v.isBlank())
			{
				continue;
			}

			Logger.debug("Inserting word link for value '{}'", v);
			WordDAO.insertLinkWord(v, courseData, session);
		}
	}

	public static void processLastScrape()
	{
		ScrapeHistory lastScrape = ScrapeHistoryDAO.getNewestScrapeHistory();

		if (lastScrape == null)
		{
			Logger.warn("Cannot index data because there is no known scrape history!");
			return;
		}

		Logger.info("Last found scrape time was: {}", lastScrape.toString());

		if (lastScrape.getHasBeenIndexed())
		{
			Logger.info("No data to process, last scrape time was already indexed.");
			return;
		}

		try (Session session = HibernateUtil.getSessionFactory().openSession())
		{
//			final String HQL1 = "SELECT cd, c, t FROM CourseData cd JOIN cd.course c JOIN c.term t";
			final String HQL1 = "FROM CourseData cd WHERE cd.scrapeId = :sid";
			List<CourseData> courseData_Course_Term = session.createQuery(HQL1, CourseData.class)
					.setParameter("sid", lastScrape).getResultList();

			int iteration = 0;
			
			for (CourseData courseData : courseData_Course_Term)
			{
				Logger.debug("At CourseData iteration: {}", iteration);
				
				iteration++;

				final String HQL2 = "FROM CourseFaculty cf WHERE cf.courseDataId = :cId";
				List<CourseFaculty> courseFaculty_List = session.createQuery(HQL2, CourseFaculty.class)
						.setParameter("cId", courseData)
						.getResultList();

				// insert all instructor names 
				for (CourseFaculty courseFaculty : courseFaculty_List)
				{
					Faculty faculty = courseFaculty.getFaculty();
					splitInsertWord(faculty.getInstructorName(), courseData, session);
					
				}
				
				
				final String HQL3 = "FROM Meeting m WHERE m.courseDataId = :cId";
				List<Meeting> meetings = session
						.createQuery(HQL3, Meeting.class)
						.setParameter("cId", courseData)
						.getResultList();

				
				// Put all unique search strings into searchWords
				for (Meeting meeting : meetings)
				{
					splitInsertWord(meeting.getBuildingDescription(), courseData, session);
				}
				
				splitInsertWord(courseData.getCourseTitle(), courseData, session);
				splitInsertWord(courseData.getSubjectLong(), courseData, session);
				splitInsertWord(courseData.getSubject(), courseData, session);
				splitInsertWord(courseData.getCampusDescription(), courseData, session);
				splitInsertWord(courseData.getInstructionalMethodDescription(), courseData, session);

			}
		}

//		ScrapeHistoryDAO.setIndexById(lastScrape.getScrapeId(), true);
		
		Logger.info("Word & Word Map | Tables Loaded-in. Successfully Completed !");
		return;
	}
}