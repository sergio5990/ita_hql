package by.it;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.transform.Transformers;
import org.hibernate.type.StandardBasicTypes;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import by.it.entity.Author;
import by.it.entity.Book;
import by.it.entity.Employee;
import by.it.util.EMUtil;

public class HQLTest {
    @Before
    public void init() {

        EntityManager em = EMUtil.getEntityManager();
        em.getTransaction().begin();

        Author author = new Author(null, "Tolstoy", new ArrayList<>());
        author.getBooks().add(new Book(null, "Alice", 1872, author));
        author.getBooks().add(new Book(null, "War & Piece", 1869, author));
        author.getBooks().add(new Book(null, "Philipok", 1865, author));

        Author pikul = new Author(null, "Pikul", new ArrayList<>());
        pikul.getBooks().add(new Book(null, "Barbarossa", 2012, pikul));
        pikul.getBooks().add(new Book(null, "Favorit", 1978, pikul));
        pikul.getBooks().add(new Book(null, "By pen & sword", 1992, pikul));

        em.persist(author);
        em.persist(pikul);

        em.persist(new Employee(null, "Yulij", 30, 8500));
        em.persist(new Employee(null, "Alex", 28, 5500));
        em.persist(new Employee(null, "Sergey", 40, 7500));
        em.persist(new Employee(null, "Yulij", 40, 9500));
        em.persist(new Employee(null, "Maria", 28, 3500));

        em.getTransaction().commit();
        em.clear();
        em.close();
    }

    @Test
    public void hql() {
        Session session = EMUtil.getSession();
        Query query = session.createQuery("from Employee");
        // timeout - в milliseconds
        query.setTimeout(1000)
                // включить в кеш запросов
                .setCacheable(true)
                // добавлять в кэш, но не считывать из него
                .setCacheMode(CacheMode.REFRESH)
                .setHibernateFlushMode(FlushMode.COMMIT)
                // сущности и коллекции помечаюся как только для чтения
                .setReadOnly(true);

        System.out.println(query.list());
    }

    @Test
    public void selectTest() {
        Session session = EMUtil.getSession();
        Query query = session.createQuery("from Employee");
        query.list().forEach(System.out::println);
    }

    @Test
    public void selectTestAlias() {
        Session session = EMUtil.getSession();
        Query query = session.createQuery("from Employee as e");
        query.list().forEach(System.out::println);
    }

    @Test
    public void selectTestClause() {
        Session session = EMUtil.getSession();
        Query query = session.createQuery("select e.name from Employee as e");
        final List<String> list = query.list();
        list.forEach(System.out::println);
    }

    @Test
    public void selectTestClauseObject() {
        Session session = EMUtil.getSession();
        Query query = session.createQuery("select a.books from Author as a");
        query.list().forEach(System.out::println);
    }

    @Test
    public void selectTestClauseObjectWhere() {
        Session session = EMUtil.getSession();
        Query query = session.createQuery("select a.books from Author as a where a.name = 'Pikul'");
        query.list().forEach(System.out::println);
    }

    @Test
    public void orderByTest() {
        Session session = EMUtil.getSession();
        Query query = session.createQuery("from Employee e order by e.salary desc");
        query.list().forEach(System.out::println);
    }

    @Test
    public void groupByTest() {
        Session session = EMUtil.getSession();
        Query query = session.createQuery("select count(e.name), e.name from Employee e group by e.name");
        query.getResultList().forEach(employees -> {
            Object[] employee = (Object[]) employees;
            System.out.println("Имя: " + employee[1] + " количество:" + employee[0]);
        });
    }

    @Test
    public void parameterTest() {
        Session session = EMUtil.getSession();
        String name = "Yulij";
        Query badQuery = session.createQuery("from Employee e where e.name = " + name);
        Query query = session.createQuery("from Employee e where e.name = :name");
        query.setParameter("name", name)
                .getResultList().forEach(System.out::println);
    }

    @Test
    public void parameterOrderTest() {
        Session session = EMUtil.getSession();
        Query query = session.createQuery(
                "from Employee e where e.name=?0 and e.salary > :salary");
        query.setParameter(0, "Yulij")
                .setParameter("salary", 5000)
                .getResultList().forEach(System.out::println);
    }

    @Test
    public void parameterListTest() {
        Session session = EMUtil.getSession();
        final List<Long> values = Arrays.asList(1L, 4L);
        Query query = session.createQuery("from Employee e where e.id in (:ids)");
        query.setParameter("ids", values)
                .getResultList().forEach(System.out::println);
    }

    @Test
    public void countDistinctTest() {
        Session session = EMUtil.getSession();
        Query query = session.createQuery("select count(distinct e.name), e.name from Employee e group by e.name");
        query.getResultList().forEach(employees -> {
            Object[] emp = (Object[]) employees;
            System.out.println("Имя: " + emp[1] + " количество:" + emp[0]);
        });
    }

    @Test
    public void like() {
        Session session = EMUtil.getSession();
        Query query = session.createQuery("from Employee e where e.name like 'Yuli' order by e.name");
        System.out.println(query.list());
    }

    @Test
    public void updateEmployee() {
        Session session = EMUtil.getSession();
        session.beginTransaction();
        session.createQuery("update Employee e set e.age = :age where name = :name")
                .setParameter("age", 21)
                .setParameter("name", "Yulij")
                .executeUpdate();
        session.getTransaction().commit();
    }

    @Test
    public void deleteTest() {
        Employee employee = new Employee(null, "Tuk", 100, 99);
        Session session = EMUtil.getSession();
        session.getTransaction().begin();
        session.persist(employee);
        session.createQuery("delete from Employee e where e.id = :id")
                .setParameter("id", employee.getId())
                .executeUpdate();
        session.getTransaction().commit();
    }

    @Test
    public void aggFun() {
        Session session = EMUtil.getSession();
        Query query = session.createQuery("select max(e.salary) from Employee e");
        System.out.println(query.list());
    }

    @Test
    public void leftJoinTest() {
        EntityManager em = EMUtil.getEntityManager();
        List<Author> authors = em.createQuery(
                "select distinct a " +
                        "from Author a " +
                        "left join a.books b " +
                        "where b.title = 'War & Piece'", Author.class)
                .getResultList();
//        System.out.println("tututu");
//        authors.forEach(author -> System.out.println(author.getName()));
        authors.forEach(System.out::println);
    }

    @Test
    public void withJoinTest() {
        Session session = EMUtil.getSession();
        List<Author> authors = session.createQuery(
                "select distinct a " +
                        "from Author a " +
                        "inner join a.books b on b.title = 'War & Piece'")
                .getResultList();
        authors.forEach(System.out::println);
    }

    @Test
    public void paginationTest() {
        final List<Employee> page0 = getPage(0);
        System.out.println("page 0");
        System.out.println(page0);

        final List<Employee> page1 = getPage(1);
        System.out.println("page 1");
        System.out.println(page1);

        final List<Employee> page2 = getPage(2);
        System.out.println("page 2");
        System.out.println(page2);
    }

    private List<Employee> getPage(int page) {
        int pageSize = 2;
        Session session = EMUtil.getSession();
        Query query = session.createQuery("from Employee e");
        return query.setMaxResults(pageSize)
                .setFirstResult(page * pageSize)
                .getResultList();
    }

    public static class Emp {
        String firstName;
        int money;

        @Override
        public String toString() {
            return "Emp{" +
                    "firstName='" + firstName + '\'' +
                    ", money=" + money +
                    '}';
        }
    }

    @Test
    public void beanName() {
        Session session = EMUtil.getSession();
        final List result = session.createSQLQuery("select e.name as firstName, e.salary as money from Employee e")
                .addScalar("firstName", StandardBasicTypes.STRING)
                .addScalar("money", StandardBasicTypes.INTEGER)
                .setResultTransformer(Transformers.aliasToBean(Emp.class)) // deprecated Jun 02, 2016
                .list();
        System.out.println(result);
    }

    public static class Cnt {
        String empName;
        int cnt;

        @Override
        public String toString() {
            return "Cnt{" +
                    "empName='" + empName + '\'' +
                    ", cnt=" + cnt +
                    '}';
        }
    }

    @Test
    public void groupByTestParams() {
        Session session = EMUtil.getSession();
        final List<Cnt> cnts = session.createSQLQuery("select count(e.name) as cnt, e.name as empName from Employee e group by e.name")
                .addScalar("cnt", StandardBasicTypes.INTEGER)
                .addScalar("empName", StandardBasicTypes.STRING)
                .setResultTransformer(Transformers.aliasToBean(Cnt.class))
                .list();
        System.out.println(cnts);


//        query.getResultList().forEach(employees -> {
//            Object[] employee = (Object[]) employees;
//            System.out.println("Имя: " + employee[1] + " количество:" + employee[0]);
//        });
    }

    @AfterClass
    public static void cleanUp() {
        EMUtil.closeEMFactory();
    }
}
