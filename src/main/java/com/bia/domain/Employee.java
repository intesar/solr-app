package com.bia.domain;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord;
import org.springframework.roo.addon.tostring.RooToString;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PreRemove;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.springframework.roo.addon.solr.RooSolrSearchable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

@Entity
@Configurable
@RooJavaBean
@RooToString
@RooJpaActiveRecord
@RooSolrSearchable
public class Employee {

    /**
     */
    @NotNull
    private String displayName;

    /**
     */
    @NotNull
    @Size(min = 3, max = 30)
    private String userName;

    /**
     */
    private String department;

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

	@Version
    @Column(name = "version")
    private Integer version;

	public Long getId() {
        return this.id;
    }

	public void setId(Long id) {
        this.id = id;
    }

	public Integer getVersion() {
        return this.version;
    }

	public void setVersion(Integer version) {
        this.version = version;
    }

	@PersistenceContext
    transient EntityManager entityManager;

	public static final EntityManager entityManager() {
        EntityManager em = new Employee().entityManager;
        if (em == null) throw new IllegalStateException("Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
        return em;
    }

	public static long countEmployees() {
        return entityManager().createQuery("SELECT COUNT(o) FROM Employee o", Long.class).getSingleResult();
    }

	public static List<Employee> findAllEmployees() {
        return entityManager().createQuery("SELECT o FROM Employee o", Employee.class).getResultList();
    }

	public static Employee findEmployee(Long id) {
        if (id == null) return null;
        return entityManager().find(Employee.class, id);
    }

	public static List<Employee> findEmployeeEntries(int firstResult, int maxResults) {
        return entityManager().createQuery("SELECT o FROM Employee o", Employee.class).setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }

	@Transactional
    public void persist() {
        if (this.entityManager == null) this.entityManager = entityManager();
        this.entityManager.persist(this);
    }

	@Transactional
    public void remove() {
        if (this.entityManager == null) this.entityManager = entityManager();
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            Employee attached = Employee.findEmployee(this.id);
            this.entityManager.remove(attached);
        }
    }

	@Transactional
    public void flush() {
        if (this.entityManager == null) this.entityManager = entityManager();
        this.entityManager.flush();
    }

	@Transactional
    public void clear() {
        if (this.entityManager == null) this.entityManager = entityManager();
        this.entityManager.clear();
    }

	@Transactional
    public Employee merge() {
        if (this.entityManager == null) this.entityManager = entityManager();
        Employee merged = this.entityManager.merge(this);
        this.entityManager.flush();
        return merged;
    }

	public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

	@Autowired
    transient SolrServer solrServer;

	public static List<Employee> search(String queryString) {
        String searchString = "Employee_solrsummary_t:" + queryString;
        return convert(search(new SolrQuery(searchString.toLowerCase())).getResults());
    }
	
	public static void deleteAll() {
        try {
            solrServer().deleteByQuery("*:*");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	public static QueryResponse search(SolrQuery query) {
        try {
            return solrServer().query(query);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new QueryResponse();
    }
	
	private static List<Employee> convert(SolrDocumentList documents) {
		List<Employee> employees = new ArrayList<Employee>();
		for (SolrDocument doc : documents) {
			Employee emp  = new Employee();
			//id, employee.displayname_s, employee.username_s, employee.department_s, employee.id_l, employee_solrsummary_t, _version_
			emp.setId(Long.parseLong(String.valueOf(doc.get("id"))));
			emp.setDisplayName(String.valueOf(doc.get("employee.displayname_s")));
			emp.setUserName(String.valueOf(doc.get("employee.username_s")));
			employees.add(emp);
		}
		return employees;
	}

	public static void indexEmployee(Employee employee) {
        List<Employee> employees = new ArrayList<Employee>();
        employees.add(employee);
        indexEmployees(employees);
    }

	@Async
    public static void indexEmployees(Collection<Employee> employees) {
        List<SolrInputDocument> documents = new ArrayList<SolrInputDocument>();
        for (Employee employee : employees) {
            SolrInputDocument sid = new SolrInputDocument();
            sid.addField("id", employee.getId());
            sid.addField("employee.displayname_s", employee.getDisplayName());
            sid.addField("employee.username_s", employee.getUserName());
            sid.addField("employee.department_s", employee.getDepartment());
            sid.addField("employee.id_l", employee.getId());
            // Add summary field to allow searching documents for objects of this type
            sid.addField("employee_solrsummary_t", new StringBuilder().append(employee.getDisplayName()).append(" ").append(employee.getUserName()).append(" ").append(employee.getDepartment()).append(" ").append(employee.getId()));
            documents.add(sid);
        }
        try {
            SolrServer solrServer = solrServer();
            solrServer.add(documents);
            solrServer.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	@Async
    public static void deleteIndex(Employee employee) {
        SolrServer solrServer = solrServer();
        try {
            solrServer.deleteById("employee_" + employee.getId());
            solrServer.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	@PostUpdate
    @PostPersist
    private void postPersistOrUpdate() {
        indexEmployee(this);
    }

	@PreRemove
    private void preRemove() {
        deleteIndex(this);
    }

	public static final SolrServer solrServer() {
        SolrServer _solrServer = new Employee().solrServer;
        if (_solrServer == null) throw new IllegalStateException("Solr server has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
        return _solrServer;
    }

	public String getDisplayName() {
        return this.displayName;
    }

	public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

	public String getUserName() {
        return this.userName;
    }

	public void setUserName(String userName) {
        this.userName = userName;
    }

	public String getDepartment() {
        return this.department;
    }

	public void setDepartment(String department) {
        this.department = department;
    }
}
