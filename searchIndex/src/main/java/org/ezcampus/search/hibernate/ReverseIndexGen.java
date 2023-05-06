package org.ezcampus.search.hibernate;

import java.util.ArrayList;
import java.util.List;

import org.ezcampus.search.System.ResourceLoader;
import org.ezcampus.search.hibernate.entity.Course;
import org.ezcampus.search.hibernate.entity.CourseData;
import org.ezcampus.search.hibernate.entity.Meeting;
import org.ezcampus.search.hibernate.entity.Term;
import org.ezcampus.search.hibernate.entity.Word;
import org.ezcampus.search.hibernate.entity.WordDAO;
import org.ezcampus.search.hibernate.util.SessionUtil;
import org.ezcampus.search.hibernate.util.WordSearchBuilder;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class ReverseIndexGen {
	
	
	public static void test ()
	{
		try (Session session = SessionUtil.getSessionFactory().openSession()) 
		{
			Transaction tx = session.getTransaction();
			
			tx.begin();
			
			session.persist(new Word("nyah"));
			session.persist(new Word("nyah1"));
			
			session.persist(new Word("hello"));
			
			session.persist(new Word("uwu"));
			session.persist(new Word("what"));
			session.persist(new Word("test"));
			
			tx.commit();
			// Get all Terms, Class types & courses:
		}
	}
	

	public static void main(String[] args) {
		ResourceLoader.loadTinyLogConfig();

		test();
		if(false) {
		
		System.out.println("Running Reverse Index Gen: ");

		//Testing the DAO
		System.out.println(WordDAO.getWord("12345"));
		
		// Step 1.

		// Open Hibernate Session
		try (Session session = SessionUtil.getSessionFactory().openSession()) {

			// Get all Terms, Class types & courses:

			List<Term> terms = session.createQuery("From Term", Term.class).list();
			List<Course> courses = session.createQuery("FROM Course", Course.class).list();

			// For all Courses, get Course_data_ids
			for (Course course : courses) {

				int id = course.getCourseId();

				// Make queries for course_data id
				List<CourseData> courseDatas = session
						.createQuery("FROM CourseData cd WHERE cd.course.courseId = :id", CourseData.class)
						.setParameter("id", id).list();

				// Course Code
				String cc = course.getCourseCode();
				
				for (CourseData cD : courseDatas) {

					int cId = cD.getCourseDataId();

					// Build Words first:

					// CRN or Course Number
					String crn = cD.getCrn();
					// Class Type (LEC, LAB, TUT)
					String classType = cD.getClassType();
					
				
					ArrayList<String> searchStrings = new ArrayList<>();

					// Meeting Location(s) -> All meetings with matching course data id
						
					List<Meeting> meetings = session.createQuery(
					    "FROM Meeting m WHERE m.courseDataId.courseDataId = :cId",
					    Meeting.class
					).setParameter("cId", cId).list(); 

					//Put all unique search strings into searchWords
					
					for (Meeting meeting : meetings) {
						
						String building = meeting.getBuildingDescription();
					    
					    if (!searchStrings.contains(building) && building != null) {
					        searchStrings.add(building);
					    }
					}

					// Course Code
					searchStrings.add( course.getCourseCode() );
					// CRN or Course Number
					searchStrings.add( cD.getCrn() );
					// Class Type (LEC, LAB, TUT)
					searchStrings.add( cD.getClassType() );
					
					//TODO: Professor of Course Data (1 CRN can only have 1 prof, so this value is STR)
					
					WordSearchBuilder build = new WordSearchBuilder(cD, searchStrings, session);
					
					System.out.println(cD.getCourseDataId());
				}
			}
		}
	}
}}