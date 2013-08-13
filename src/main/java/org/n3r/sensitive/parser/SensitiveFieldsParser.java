package org.n3r.sensitive.parser;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLCallStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleDeleteStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleInsertStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleMergeStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleSelectTableReference;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleUpdateStatement;
import com.alibaba.druid.sql.dialect.oracle.parser.OracleStatementParser;
import com.alibaba.druid.sql.dialect.oracle.visitor.OracleASTVisitorAdapter;
import com.alibaba.druid.sql.parser.ParserException;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class SensitiveFieldsParser extends OracleASTVisitorAdapter {
    private final Logger logger = LoggerFactory.getLogger(SensitiveFieldsParser.class);

    private final Map<String, String> aliasTablesMap = Maps.newHashMap();
    private final List<Integer> securetBindIndice = Lists.newArrayList();
    private final List<Integer> securetResultIndice = Lists.newArrayList();
    private final List<String> securetResultLabels = Lists.newArrayList();
    private final Set<String> securetFields;

    private int variantIndex = 1;
    private final String sql;

    public static SensitiveFieldsParser parseSecuretFields(String sql) {
        return parseSecuretFields(sql, SensitiveFieldsConfig.CONFIG);
    }

    public static SensitiveFieldsParser parseSecuretFields(String sql, Set<String> securetFields) {
       SQLStatementParser parser = new OracleStatementParser(sql);
        List<SQLStatement> stmtList;
        try {
            stmtList = parser.parseStatementList();
        } catch (ParserException exception) {
            throw new RuntimeException(sql + " is invalid, detail " + exception.getMessage());
        }

        SQLStatement sqlStatement = stmtList.get(0);
        SensitiveFieldsParser visitorAdapter = null;

        if (sqlStatement instanceof SQLSelectStatement) {
            SQLSelectStatement selectStatement = (SQLSelectStatement) sqlStatement;
            SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) selectStatement.getSelect().getQuery();

            visitorAdapter = new SensitiveFieldsParser(queryBlock, securetFields, sql);
        } else if (sqlStatement instanceof OracleDeleteStatement) {
            visitorAdapter = new SensitiveFieldsParser((OracleDeleteStatement) sqlStatement, securetFields, sql);
        } else if (sqlStatement instanceof OracleInsertStatement) {
            visitorAdapter = new SensitiveFieldsParser((OracleInsertStatement) sqlStatement, securetFields, sql);
        } else if (sqlStatement instanceof OracleUpdateStatement) {
            visitorAdapter = new SensitiveFieldsParser((OracleUpdateStatement) sqlStatement, securetFields, sql);
        } else if (sqlStatement instanceof OracleMergeStatement) {
            visitorAdapter = new SensitiveFieldsParser((OracleMergeStatement) sqlStatement, securetFields, sql);
        } else if (sqlStatement instanceof SQLCallStatement) {
            visitorAdapter = new SensitiveFieldsParser((SQLCallStatement) sqlStatement, securetFields, sql);
        }

        return visitorAdapter;
    }

    public SensitiveFieldsParser(SQLSelectQueryBlock queryBlock, Set<String> securetFields, String sql) {
        this.securetFields = securetFields;
        this.sql = sql;
        parseTable(queryBlock.getFrom());
        parseSelectItems(queryBlock.getSelectList());

        if (queryBlock.getWhere() != null) queryBlock.getWhere().accept(this);
    }

    public SensitiveFieldsParser(OracleInsertStatement insertStatement, Set<String> securetFields, String sql) {
        this.securetFields = securetFields;
        this.sql = sql;
        parseInsert(insertStatement);
    }

    public SensitiveFieldsParser(OracleUpdateStatement updateStatement, Set<String> securetFields, String sql) {
        this.securetFields = securetFields;
        this.sql = sql;
        parseUpdate(updateStatement);

        if (updateStatement.getWhere() != null) updateStatement.getWhere().accept(this);
    }

    public SensitiveFieldsParser(OracleMergeStatement mergeStatement, Set<String> securetFields, String sql) {
        this.securetFields = securetFields;
        this.sql = sql;
        parseMerge(mergeStatement);
    }

    public SensitiveFieldsParser(SQLCallStatement callStatement, Set<String> securetFields, String sql) {
        this.securetFields = securetFields;
        this.sql = sql;
        parseCall(callStatement);
    }

    public SensitiveFieldsParser(OracleDeleteStatement sqlStatement, Set<String> securetFields, String sql) {
        this.securetFields = securetFields;
        this.sql = sql;
        parseDelete(sqlStatement);
    }

    private void parseDelete(OracleDeleteStatement deleteStatement) {
        SQLExprTableSource tableSource = (SQLExprTableSource) deleteStatement.getTableSource();
        if (tableSource.getExpr() instanceof SQLIdentifierExpr)
            addTableAlias(tableSource, (SQLIdentifierExpr) tableSource.getExpr());

        if (deleteStatement.getWhere() != null) deleteStatement.getWhere().accept(this);
    }

    private void parseCall(SQLCallStatement callStatement) {
        addTableAlias("", callStatement.getProcedureName().toString());

        List<SQLExpr> parameters = callStatement.getParameters();
        for (int i = 0, ii = parameters.size(); i < ii; ++i) {
            SQLExpr parameter = parameters.get(i);
            parameter.accept(this);

            if (!isSecuretField(i + 1)) continue;
            if (parameter instanceof SQLVariantRefExpr) {
                securetBindIndice.add(variantIndex - 1);
            } else {
                logger.warn("securet field is not passed as a single value in sql [" + sql + "]");
            }
        }
    }

    private void parseMerge(OracleMergeStatement mergeStatement) {
        if (mergeStatement.getInto() instanceof SQLIdentifierExpr) {
            SQLIdentifierExpr expr = (SQLIdentifierExpr) mergeStatement.getInto();
            addTableAlias(mergeStatement.getAlias(), expr);
        }

        OracleMergeStatement.MergeUpdateClause updateClause = mergeStatement.getUpdateClause();
        if (updateClause != null) {
            List<SQLUpdateSetItem> items = updateClause.getItems();
            walkUpdateItems(items);
        }

        OracleMergeStatement.MergeInsertClause insertClause = mergeStatement.getInsertClause();
        if (insertClause != null) {
            List<Integer> securetFieldsIndice = walkInsertColumns(insertClause.getColumns());
            walkInsertValues(securetFieldsIndice, insertClause.getValues());
        }
    }

    private void parseUpdate(OracleUpdateStatement updateStatement) {
        OracleSelectTableReference tableSource = (OracleSelectTableReference) updateStatement.getTableSource();
        if (tableSource.getExpr() instanceof SQLIdentifierExpr)
            addTableAlias(tableSource, (SQLIdentifierExpr) tableSource.getExpr());

        List<SQLUpdateSetItem> items = updateStatement.getItems();
        walkUpdateItems(items);
    }

    private void walkUpdateItems(List<SQLUpdateSetItem> items) {
        for (int i = 0, ii = items.size(); i < ii; ++i) {
            SQLUpdateSetItem item = items.get(i);
            item.accept(this);

            boolean isSecuretField = false;
            if (item.getColumn() instanceof SQLPropertyExpr) {
                SQLPropertyExpr expr = (SQLPropertyExpr) item.getColumn();
                isSecuretField = isSecuretField(expr);
            } else if (item.getColumn() instanceof SQLIdentifierExpr) {
                isSecuretField = isSecuretField((SQLIdentifierExpr) item.getColumn());
            }

            if (!isSecuretField) continue;

            if (item.getValue() instanceof SQLVariantRefExpr) {
                securetBindIndice.add(variantIndex - 1);
            } else {
                logger.warn("securet field is not updated as a single value in sql [" + sql + "]");
            }
        }
    }

    @Override
    public boolean visit(SQLVariantRefExpr x) {
        ++variantIndex;
        return true;
    }

    @Override
    public boolean visit(SQLBinaryOpExpr x) {
        SQLExpr left = x.getLeft();
        SQLExpr right = x.getRight();
        if (left instanceof SQLIdentifierExpr && right instanceof SQLVariantRefExpr) {
            if (isSecuretField((SQLIdentifierExpr) left)) securetBindIndice.add(variantIndex);
        } else if (left instanceof SQLPropertyExpr && right instanceof SQLVariantRefExpr) {
            SQLPropertyExpr leftExpr = (SQLPropertyExpr) left;
            if (isSecuretField(leftExpr)) securetBindIndice.add(variantIndex);
        }
        return true;
    }

    private boolean isSecuretField(SQLIdentifierExpr field) {
        String oneTableName = getOneTableName();
        return oneTableName != null && securetFields.contains(oneTableName + "." + field.getName());
    }

    private boolean isSecuretField(SQLPropertyExpr expr) {
        String tableName = aliasTablesMap.get(expr.getOwner().toString());
        String fieldName = expr.getName();
        return securetFields.contains(tableName + "." + fieldName);
    }


    private boolean isSecuretField(int procedureParameterIndex) {
        String oneTableName = getOneTableName();
        return oneTableName != null && securetFields.contains(oneTableName + "." + procedureParameterIndex);
    }

    private String getOneTableName() {
        if (aliasTablesMap.size() == 1)
            for (Map.Entry<String, String> entry : aliasTablesMap.entrySet())
                return entry.getValue();

        return null;
    }

    private void parseTable(SQLTableSource from) {
        if (from instanceof OracleSelectTableReference) {
            SQLExprTableSource source = (SQLExprTableSource) from;

            if (source.getExpr() instanceof SQLIdentifierExpr)
                addTableAlias(from, (SQLIdentifierExpr) source.getExpr());

        } else if (from instanceof SQLJoinTableSource) {
            SQLJoinTableSource joinTableSource = (SQLJoinTableSource) from;
            parseTable(joinTableSource.getLeft());
            parseTable(joinTableSource.getRight());
        }
    }

    private void addTableAlias(SQLTableSource from, SQLIdentifierExpr expr) {
        addTableAlias(from.getAlias(), expr);
    }

    private void addTableAlias(String alias, SQLIdentifierExpr expr) {
        addTableAlias(alias, expr.getName());
    }

    private void addTableAlias(String alias, String tableName) {
        aliasTablesMap.put(Objects.firstNonNull(alias, tableName), tableName);
    }

    private void parseSelectItems(List<SQLSelectItem> sqlSelectItems) {
        for (int itemIndex = 0, ii = sqlSelectItems.size(); itemIndex < ii; ++itemIndex) {
            SQLSelectItem item = sqlSelectItems.get(itemIndex);
            String alias = item.getAlias();

            if (item.getExpr() instanceof SQLIdentifierExpr) {
                SQLIdentifierExpr expr = (SQLIdentifierExpr) item.getExpr();

                if (isSecuretField(expr)) {
                    securetResultIndice.add(itemIndex + 1);
                    securetResultLabels.add(alias == null ? expr.getName() : alias);
                }

            } else if (item.getExpr() instanceof SQLPropertyExpr) {
                SQLPropertyExpr expr = (SQLPropertyExpr) item.getExpr();
                if (isSecuretField(expr)) {
                    securetResultIndice.add(itemIndex + 1);
                    securetResultLabels.add(alias == null ? expr.getName() : alias);
                }
            }
        }
    }

    private void parseInsert(OracleInsertStatement x) {
        SQLExprTableSource tableSource = x.getTableSource();
        if (tableSource.getExpr() instanceof SQLIdentifierExpr)
            addTableAlias(tableSource, (SQLIdentifierExpr) tableSource.getExpr());

        List<SQLExpr> columns = x.getColumns();
        List<Integer> securetFieldsIndice = walkInsertColumns(columns);

        List<SQLExpr> values = x.getValues().getValues();

        walkInsertValues(securetFieldsIndice, values);
    }

    private void walkInsertValues(List<Integer> securetFieldsIndice, List<SQLExpr> values) {
        for (int i = 0, ii = values.size(); i < ii; ++i) {
            SQLExpr expr = values.get(i);
            expr.accept(this);
            if (securetFieldsIndice.contains(i)) {
                if (expr instanceof SQLVariantRefExpr) securetBindIndice.add(variantIndex - 1);
                else logger.warn("securet field is not inserted as a single value in sql [" + sql + "]");
            }
        }
    }

    private List<Integer> walkInsertColumns(List<SQLExpr> columns) {
        List<Integer> securetFieldsIndice = Lists.newArrayList();
        for (int i = 0, ii = columns.size(); i < ii; ++i) {
            SQLExpr column = columns.get(i);
            if (column instanceof SQLIdentifierExpr) {
                SQLIdentifierExpr expr = (SQLIdentifierExpr) column;
                if (isSecuretField(expr)) securetFieldsIndice.add(i);
            }
        }

        return securetFieldsIndice;
    }

    public List<Integer> getSecuretBindIndice() {
        return securetBindIndice;
    }

    public List<Integer> getSecuretResultIndice() {
        return securetResultIndice;
    }

    public List<String> getSecuretResultLabels() {
        return securetResultLabels;
    }

    public boolean inBindIndice(Integer index) {
        return getSecuretBindIndice().contains(index);
    }

    public boolean inResultIndice(Object index) {
        return getSecuretResultIndice().contains(index)
                || getSecuretResultLabels().contains(index);
    }

    public boolean haveSecureFields() {
        return securetResultLabels.size() > 0
                ||securetResultIndice.size() > 0
                || securetBindIndice.size() > 0;
    }
}
