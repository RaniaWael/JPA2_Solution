package com.sumerge.grad.program.jpa.rest.student;

import com.sumerge.grad.program.jpa.repositories.boundary.Repository;
import com.sumerge.grad.program.jpa.repositories.entity.Address;
import com.sumerge.grad.program.jpa.repositories.entity.Course;
import com.sumerge.grad.program.jpa.repositories.entity.Instructor;
import com.sumerge.grad.program.jpa.repositories.entity.Student;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import javax.transaction.UserTransaction;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static com.sumerge.grad.program.jpa.constants.Constants.PERSISTENT_UNIT;
import static java.util.logging.Level.SEVERE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@RequestScoped
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Path("students")
public class StudentResources {

    private static final Logger LOGGER = Logger.getLogger(StudentResources.class.getName());

    @EJB
    private Repository repo;

    @PersistenceContext(unitName = PERSISTENT_UNIT)
    private EntityManager em;

    @Context
    HttpServletRequest request;

    @GET
    public Response getAllStudents() {
        try {
            return Response.ok().
                    entity(em.createQuery("SELECT s FROM Student s" +
                            " LEFT JOIN FETCH s.addresses", Student.class).
                            getResultList()).
                    build();
        } catch (Exception e) {
            LOGGER.log(SEVERE, e.getMessage(), e);
            return Response.serverError().
                    entity(e).
                    build();
        }
    }

    @GET
    @Path("{id}")
    public Response getStudent(@PathParam("id") Long id) {
        try {
            return Response.ok().
                    entity(em.find(Student.class, id)).
                    build();
        } catch (Exception e) {
            LOGGER.log(SEVERE, e.getMessage(), e);
            return Response.serverError().
                    entity(e).
                    build();
        }
    }

    @GET
    @Path("takingCourse/{course}")
    public Response getStudentTakingX(@PathParam("course") String course) {
        try {
            return Response.ok().
                    entity(em.createQuery("SELECT s FROM Student s JOIN FETCH s.courses c WHERE c.name = :course").setParameter("course", course).getResultList()).
                    build();
        } catch (Exception e) {
            LOGGER.log(SEVERE, e.getMessage(), e);
            return Response.serverError().
                    entity(e).
                    build();
        }
    }

    @GET
    @Path("totalHours/{id}")
    public Response totalHours(@PathParam("id") int id) {
        try {
            return Response.ok().
                    entity(em.createQuery("SELECT SUM(c.hours) FROM Student s JOIN FETCH s.courses c WHERE s.id = :id").setParameter("id", id).getSingleResult()).
                    build();
        } catch (Exception e) {
            LOGGER.log(SEVERE, e.getMessage(), e);
            return Response.serverError().
                    entity(e).
                    build();
        }
    }

    @GET
    @Path("livingIn/{city}")
    public Response livingIn(@PathParam("city") String city) {
        try {
            return Response.ok().
                    entity(em.createQuery("SELECT s FROM Student s JOIN FETCH s.addresses a WHERE a.city = :city").setParameter("city", city).getResultList()).
                    build();
        } catch (Exception e) {
            LOGGER.log(SEVERE, e.getMessage(), e);
            return Response.serverError().
                    entity(e).
                    build();
        }
    }

    @GET
    @Path("countLivingIn/{city}")
    public Response countLivingIn(@PathParam("city") String city) {
        try {
            return Response.ok().
                    entity(em.createQuery("SELECT COUNT(s) FROM Student s JOIN FETCH s.addresses a WHERE a.city = :city").setParameter("city", city).getResultList()).
                    build();
        } catch (Exception e) {
            LOGGER.log(SEVERE, e.getMessage(), e);
            return Response.serverError().
                    entity(e).
                    build();
        }
    }

    @GET
    @Path("modifyAddr/{id}")
    @Transactional
    public Response modifyAddr(@PathParam("id") int id) {
        try {
            //em.joinTransaction();
            return Response.ok().
                    entity(em.createQuery("UPDATE Address a SET a.city = 'Giza' WHERE a.studentID = :id").setParameter("id", id).executeUpdate()).
                    build();
        } catch (Exception e) {
            LOGGER.log(SEVERE, e.getMessage(), e);
            return Response.serverError().
                    entity(e).
                    build();
        }
    }

    @GET
    @Path("addNewAddr")
    public Response addAddr() {
        try {
            UserTransaction transaction = (UserTransaction)new InitialContext().lookup("java:comp/UserTransaction");
            transaction.begin();
            Address newAddr = new Address();
            newAddr.setCity("Aswan");
            newAddr.setCountry("EG");
            newAddr.setStreetAddress("Cornesh");
            newAddr.setStudentID(2);

            em.persist(newAddr);
            transaction.commit();
            return Response.ok().build();
        } catch (Exception e) {
            LOGGER.log(SEVERE, e.getMessage(), e);
            return Response.serverError().
                    entity(e).
                    build();
        }
    }

    @GET
    @Path("criteria-builder")
    public Response cb() {
        return criteriaBuilder(null, "ML", null, "EG", "Cairo");
    }

    public Response criteriaBuilder(String name, String courseName, String instructor, String country, String city) {
        CriteriaBuilder c = em.getCriteriaBuilder();
        CriteriaQuery<Student> criteriaQuery = c.createQuery(Student.class);
        Root<Student> studentEntity = criteriaQuery.from(Student.class);

        // Add the predicates
        criteriaQuery.select(studentEntity);
        List<Predicate> preds = new ArrayList<>();
        if (name != null)
            preds.add(c.equal(studentEntity.<String>get("name"),  name));
        if (courseName != null) {
            Join<Student, Course> courses = studentEntity.join("courses");
            preds.add(c.equal(courses.get("name"), courseName));
        }

//        if (instructor != null) {
//            Join<Student, Instructor> stu_instr = studentEntity.join("courses", JoinType.INNER);
//            preds.add(c.equal(stu_instr.<String>get("name"), instructor));
//        }

        Join<Student, Course> addresses = studentEntity.join("addresses");

        if (country != null)
            preds.add(c.equal(addresses.get("country"), country));

        if (city != null)
            preds.add(c.equal(addresses.get("city"), city));

        // Executing all predicates
        criteriaQuery.where(c.and(preds.toArray(new Predicate[preds.size()])));
        TypedQuery<Student> query = em.createQuery(criteriaQuery);
        return Response.ok().entity(query.getResultList()).build();

    }
    @POST
    public Response addStudent(Student student) {
        try {
            if (student.getId() != null)
                throw new IllegalArgumentException("Can't create student since it exists in the database");

            Student merged = (Student) repo.save(student);
            URI uri = new URI(request.getContextPath() + "students" + merged.getId());
            return Response.created(uri).
                    build();
        } catch (Exception e) {
            LOGGER.log(SEVERE, e.getMessage(), e);
            return Response.serverError().
                    entity(e).
                    build();
        }
    }

    @PUT
    public Response editStudent(Student student) {
        try {
            if (student.getId() == null)
                throw new IllegalArgumentException("Can't edit student since it does not exist in the database");

            repo.save(student);
            return Response.ok().
                    build();
        } catch (Exception e) {
            LOGGER.log(SEVERE, e.getMessage(), e);
            return Response.serverError().
                    entity(e).
                    build();
        }
    }
}
