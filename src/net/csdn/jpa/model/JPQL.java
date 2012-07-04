package net.csdn.jpa.model;

import net.csdn.common.logging.CSLogger;
import net.csdn.common.logging.Loggers;
import net.csdn.jpa.context.JPAContext;
import net.csdn.jpa.model.GenericModel.JPAQuery;
import org.apache.commons.beanutils.BeanUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JPQL {
    private CSLogger logger = Loggers.getLogger(getClass());
    private final JPAContext jpaContext;
    private String sql = "";

    private String where = "";
    private String select = "";
    private String joins = "";
    private String order = "";
    private int limit = -1;
    private int offset = 0;
    private Map<String, Object> bindings = new HashMap();
    private static String EMPTY_STRING = " ";
    private String entity = "";

    public JPQL(JPAContext jpaContext) {
        this.jpaContext = jpaContext;
    }

    public JPQL(JPAContext jpaContext, String entity) {
        this.jpaContext = jpaContext;
        this.entity = entity;
    }


    public EntityManager em() {
        return jpaContext.em();
    }

    //下面这些方法都是模拟active_record的链式操作
    public JPQL where(String condition, Map params) {
        this.where = "where" + EMPTY_STRING + condition;
        this.bindings = params;
        return this;
    }

    public JPQL where(String condition) {
        this.where = "where" + EMPTY_STRING + condition;
        return this;
    }

    public JPQL select(String select) {
        this.select = select;
        return this;
    }

    public JPQL joins(String joins) {

        this.joins = joins;
        return this;
    }

    public JPQL order(String order) {
        this.order = "order by " + EMPTY_STRING + order;
        return this;
    }

    public JPQL limit(int limit) {
        this.limit = limit;
        return this;
    }

    public JPQL offset(int offset) {
        this.offset = offset;
        return this;
    }

    public List fetch() {
        sql = select + EMPTY_STRING + "from" + EMPTY_STRING + entity + EMPTY_STRING + where + EMPTY_STRING + joins + EMPTY_STRING + order + EMPTY_STRING;
        //limit 1.取一条
        Query query = em().createQuery(sql);

        for (String obj : bindings.keySet()) {
            query.setParameter(obj, bindings.get(obj));
        }
        query.setFirstResult(offset);
        if (limit != -1) query.setMaxResults(limit);
        return query.getResultList();
    }
    //现在暂时实现上面这几个,其实应该差不多了 哈哈


    public long count(String entity) {
        return Long.parseLong(em().createQuery("select count(*) from " + entity + " e").getSingleResult().toString());
    }

    public long count(String entity, String query, Object[] params) {
        return Long.parseLong(
                bindParameters(em().createQuery(
                        createCountQuery(entity, entity, query, params)), params).getSingleResult().toString());
    }

    public List findAll(String entity) {
        return em().createQuery("select e from " + entity + " e").getResultList();
    }

    public JPABase findById(Class clzz, Object id) throws Exception {
        JPABase model = (JPABase) em().find(clzz, id);
        return model;
    }

    public List findBy(String entity, String query, Object[] params) {
        Query q = em().createQuery(
                createFindByQuery(entity, entity, query, params));
        return bindParameters(q, params).getResultList();
    }

    public JPAQuery find(String entity, String query, Object[] params) {
        Query q = em().createQuery(
                createFindByQuery(entity, entity, query, params));
        return new JPAQuery(
                createFindByQuery(entity, entity, query, params), bindParameters(q, params));
    }

    public JPAQuery find(String entity) {
        Query q = em().createQuery(
                createFindByQuery(entity, entity, null));
        return new JPAQuery(
                createFindByQuery(entity, entity, null), bindParameters(q));
    }

    public JPAQuery all(String entity) {
        Query q = em().createQuery(
                createFindByQuery(entity, entity, null));
        return new JPAQuery(
                createFindByQuery(entity, entity, null), bindParameters(q));
    }

    public int delete(String entity, String query, Object[] params) {
        Query q = em().createQuery(
                createDeleteQuery(entity, entity, query, params));
        return bindParameters(q, params).executeUpdate();
    }

    public int deleteAll(String entity) {
        Query q = em().createQuery(
                createDeleteQuery(entity, entity, null));
        return bindParameters(q).executeUpdate();
    }

    public JPABase findOneBy(String entity, String query, Object[] params) {
        Query q = em().createQuery(
                createFindByQuery(entity, entity, query, params));
        List results = bindParameters(q, params).getResultList();
        if (results.size() == 0) {
            return null;
        }
        return (JPABase) results.get(0);
    }

    public JPABase create(Class clzz, Map<Object, Object> params) throws Exception {
        Object o = clzz.newInstance();
        for (Map.Entry<Object, Object> entry : params.entrySet()) {
            BeanUtils.setProperty(o, entry.getKey().toString(), entry.getValue());
        }
        return ((GenericModel) o);
    }


    public String createFindByQuery(String entityName, String entityClass, String query, Object... params) {
        if (query == null || query.trim().length() == 0) {
            return "from " + entityName;
        }
        if (query.matches("^by[A-Z].*$")) {
            return "from " + entityName + " where " + findByToJPQL(query);
        }
        if (query.trim().toLowerCase().startsWith("select ")) {
            return query;
        }
        if (query.trim().toLowerCase().startsWith("from ")) {
            return query;
        }
        if (query.trim().toLowerCase().startsWith("order by ")) {
            return "from " + entityName + " " + query;
        }
        if (query.trim().indexOf(' ') == -1 && query.trim().indexOf('=') == -1 && params != null && params.length == 1) {
            query += " = ?1";
        }
        if (query.trim().indexOf(' ') == -1 && query.trim().indexOf('=') == -1 && params == null) {
            query += " = null";
        }
        return "from " + entityName + " where " + query;
    }

    public String createDeleteQuery(String entityName, String entityClass, String query, Object... params) {
        if (query == null) {
            return "delete from " + entityName;
        }
        if (query.trim().toLowerCase().startsWith("delete ")) {
            return query;
        }
        if (query.trim().toLowerCase().startsWith("from ")) {
            return "delete " + query;
        }
        if (query.trim().indexOf(' ') == -1 && query.trim().indexOf('=') == -1 && params != null && params.length == 1) {
            query += " = ?1";
        }
        if (query.trim().indexOf(' ') == -1 && query.trim().indexOf('=') == -1 && params == null) {
            query += " = null";
        }
        return "delete from " + entityName + " where " + query;
    }

    public String createCountQuery(String entityName, String entityClass, String query, Object... params) {
        if (query.trim().toLowerCase().startsWith("select ")) {
            return query;
        }
        if (query.matches("^by[A-Z].*$")) {
            return "select count(*) from " + entityName + " where " + findByToJPQL(query);
        }
        if (query.trim().toLowerCase().startsWith("from ")) {
            return "select count(*) " + query;
        }
        if (query.trim().toLowerCase().startsWith("order by ")) {
            return "select count(*) from " + entityName;
        }
        if (query.trim().indexOf(' ') == -1 && query.trim().indexOf('=') == -1 && params != null && params.length == 1) {
            query += " = ?1";
        }
        if (query.trim().indexOf(' ') == -1 && query.trim().indexOf('=') == -1 && params == null) {
            query += " = null";
        }
        if (query.trim().length() == 0) {
            return "select count(*) from " + entityName;
        }
        return "select count(*) from " + entityName + " e where " + query;
    }

    @SuppressWarnings("unchecked")
    public Query bindParameters(Query q, Object... params) {
        if (params == null) {
            return q;
        }
        if (params.length == 1 && params[0] instanceof Map) {
            return bindParameters(q, (Map<String, Object>) params[0]);
        }
        for (int i = 0; i < params.length; i++) {
            q.setParameter(i + 1, params[i]);
        }
        return q;
    }

    public Query bindParameters(Query q, Map<String, Object> params) {
        if (params == null) {
            return q;
        }
        for (String key : params.keySet()) {
            q.setParameter(key, params.get(key));
        }
        return q;
    }

    public String findByToJPQL(String findBy) {
        findBy = findBy.substring(2);
        StringBuilder jpql = new StringBuilder();
        String subRequest;
        if (findBy.contains("OrderBy"))
            subRequest = findBy.split("OrderBy")[0];
        else subRequest = findBy;
        String[] parts = subRequest.split("And");
        int index = 1;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.endsWith("NotEqual")) {
                String prop = extractProp(part, "NotEqual");
                jpql.append(prop).append(" <> ?").append(index++);
            } else if (part.endsWith("Equal")) {
                String prop = extractProp(part, "Equal");
                jpql.append(prop).append(" = ?").append(index++);
            } else if (part.endsWith("IsNotNull")) {
                String prop = extractProp(part, "IsNotNull");
                jpql.append(prop).append(" is not null");
            } else if (part.endsWith("IsNull")) {
                String prop = extractProp(part, "IsNull");
                jpql.append(prop).append(" is null");
            } else if (part.endsWith("LessThan")) {
                String prop = extractProp(part, "LessThan");
                jpql.append(prop).append(" < ?").append(index++);
            } else if (part.endsWith("LessThanEquals")) {
                String prop = extractProp(part, "LessThanEquals");
                jpql.append(prop).append(" <= ?").append(index++);
            } else if (part.endsWith("GreaterThan")) {
                String prop = extractProp(part, "GreaterThan");
                jpql.append(prop).append(" > ?").append(index++);
            } else if (part.endsWith("GreaterThanEquals")) {
                String prop = extractProp(part, "GreaterThanEquals");
                jpql.append(prop).append(" >= ?").append(index++);
            } else if (part.endsWith("Between")) {
                String prop = extractProp(part, "Between");
                jpql.append(prop).append(" < ?").append(index++).append(" AND ").append(prop).append(" > ?").append(index++);
            } else if (part.endsWith("Like")) {
                String prop = extractProp(part, "Like");
                jpql.append("LOWER(").append(prop).append(") like ?").append(index++);
            } else if (part.endsWith("Ilike")) {
                String prop = extractProp(part, "Ilike");
                jpql.append("LOWER(").append(prop).append(") like LOWER(?").append(index++).append(")");
            } else if (part.endsWith("Elike")) {
                String prop = extractProp(part, "Elike");
                jpql.append(prop).append(" like ?").append(index++);
            } else {
                String prop = extractProp(part, "");
                jpql.append(prop).append(" = ?").append(index++);
            }
            if (i < parts.length - 1) {
                jpql.append(" AND ");
            }
        }
        // ORDER BY clause
        if (findBy.contains("OrderBy")) {
            jpql.append(" ORDER BY ");
            String orderQuery = findBy.split("OrderBy")[1];
            parts = orderQuery.split("And");
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                String orderProp;
                if (part.endsWith("Desc"))
                    orderProp = extractProp(part, "Desc") + " DESC";
                else orderProp = part.toLowerCase();
                if (i > 0)
                    jpql.append(", ");
                jpql.append(orderProp);
            }
        }
        return jpql.toString();
    }


    protected static String extractProp(String part, String end) {
        String prop = part.substring(0, part.length() - end.length());
        prop = (prop.charAt(0) + "").toLowerCase() + prop.substring(1);
        return prop;
    }

}