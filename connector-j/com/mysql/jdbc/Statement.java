/*
   Copyright (C) 2002 MySQL AB
   
      This program is free software; you can redistribute it and/or modify
      it under the terms of the GNU General Public License as published by
      the Free Software Foundation; either version 2 of the License, or
      (at your option) any later version.
   
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
   
      You should have received a copy of the GNU General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
      
 */

package com.mysql.jdbc;

import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Types;

import java.util.ArrayList;

/**
 * A Statement object is used for executing a static SQL statement and
 * obtaining the results produced by it.
 *
 * <p>Only one ResultSet per Statement can be open at any point in time.
 * Therefore, if the reading of one ResultSet is interleaved with the
 * reading of another, each must have been generated by different
 * Statements.  All statement execute methods implicitly close a
 * statement's current ResultSet if an open one exists.
 *
 * @see java.sql.Statement
 * @see ResultSet
 * @author Mark Matthews
 * @version $Id$
 */
public class Statement
    implements java.sql.Statement {

    //~ Instance/static variables .............................................

    /**
     * Holds batched commands
     */
    protected ArrayList batchedArgs;

    /**
     * The connection who created us
     */
    protected Connection connection = null;
    protected int resultSetConcurrency = 0;
    protected int resultSetType = 0;
    protected String currentCatalog = null;

    /**
     * Should we process escape codes?
     */
    protected boolean doEscapeProcessing = true;

    /**
     * Processes JDBC escape codes
     */
    protected EscapeProcessor escaper = null;
    protected long lastInsertId = -1;
    protected int maxFieldSize = MysqlIO.getMaxBuf();
    protected int maxRows = -1;

    /**
     * The next result set
     */
    protected ResultSet nextResults = null;

    /**
     * The current results
     */
    protected ResultSet results = null;

    /**
     * The timeout for a query
     */
    protected int timeout = 0;
    protected long updateCount = -1;

    /**
     * The warnings chain.
     */
    protected SQLWarning warningChain = null;
    private int fetchSize = 0;
    protected boolean isClosed = false;

    //~ Constructors ..........................................................

    /**
     * Constructor for a Statement.  It simply sets the connection
     * that created us.
     *
     * @param c the Connection instantation that creates us
     */
    public Statement(Connection c, String catalog)
              throws SQLException {

        if (Driver.TRACE) {

            Object[] args = { c };
            Debug.methodCall(this, "constructor", args);
        }

        if (c == null || ((com.mysql.jdbc.Connection) c).isClosed()) {
            throw new SQLException("Connection is closed.", "08003");
        }

        connection = c;
        escaper = new EscapeProcessor();
        currentCatalog = catalog;

        //
        // Adjust, if we know it
        //
        if (connection != null) {
            maxFieldSize = connection.getMaxAllowedPacket();
        }
    }

    //~ Methods ...............................................................

    /**
     * JDBC 2.0
     * 
     * Return the Connection that produced the Statement.
     */
    public java.sql.Connection getConnection()
                                      throws SQLException {

        return (java.sql.Connection) connection;
    }

    /**
     * setCursorName defines the SQL cursor name that will be used by
     * subsequent execute methods.  This name can then be used in SQL
     * positioned update/delete statements to identify the current row
     * in the ResultSet generated by this statement.  If a database
     * doesn't support positioned update/delete, this method is a
     * no-op.
     *
     * <p><b>Note:</b> This MySQL driver does not support cursors.
     *
     *
     * @param name the new cursor name
     * @exception java.sql.SQLException if a database access error occurs
     */
    public void setCursorName(String name)
                       throws java.sql.SQLException {

        if (Driver.TRACE) {

            Object[] args = { name };
            Debug.methodCall(this, "setCursorName", args);
        }

        // No-op
    }

    /**
     * If escape scanning is on (the default), the driver will do escape
     * substitution before sending the SQL to the database.
     *
     * @param enable true to enable; false to disable
     * @exception java.sql.SQLException if a database access error occurs
     */
    public void setEscapeProcessing(boolean enable)
                             throws java.sql.SQLException {

        if (Driver.TRACE) {

            Object[] args = { new Boolean(enable) };
            Debug.methodCall(this, "setEscapeProcessing", args);
        }

        doEscapeProcessing = enable;
    }

    //--------------------------JDBC 2.0-----------------------------

    /**
     * JDBC 2.0
     *
     * Give a hint as to the direction in which the rows in a result set
     * will be processed. The hint applies only to result sets created 
     * using this Statement object.  The default value is 
     * ResultSet.FETCH_FORWARD.
     *
     * @param direction the initial direction for processing rows
     * @exception SQLException if a database-access error occurs or direction
     * is not one of ResultSet.FETCH_FORWARD, ResultSet.FETCH_REVERSE, or
     * ResultSet.FETCH_UNKNOWN
     */
    public void setFetchDirection(int direction)
                           throws SQLException {

        switch (direction) {

            case java.sql.ResultSet.FETCH_FORWARD:
            case java.sql.ResultSet.FETCH_REVERSE:
            case java.sql.ResultSet.FETCH_UNKNOWN:
                break;

            default:
                throw new SQLException("Illegal value for setFetchDirection()", 
                                       "S1009");
        }
    }

    /**
     * JDBC 2.0
     *
     * Determine the fetch direction.
     *
     * @return the default fetch direction
     * @exception SQLException if a database-access error occurs
     */
    public int getFetchDirection()
                          throws SQLException {

        return java.sql.ResultSet.FETCH_FORWARD;
    }

    /**
     * JDBC 2.0
     *
     * Give the JDBC driver a hint as to the number of rows that should 
     * be fetched from the database when more rows are needed.  The number 
     * of rows specified only affects result sets created using this 
     * statement. If the value specified is zero, then the hint is ignored.
     * The default value is zero.
     *
     * @param rows the number of rows to fetch
     * @exception SQLException if a database-access error occurs, or the
     * condition 0 <= rows <= this.getMaxRows() is not satisfied.
     */
    public void setFetchSize(int rows)
                      throws SQLException {

        if ((rows < 0 && rows != Integer.MIN_VALUE)
            || (maxRows != 0 
            && maxRows != -1 
            && rows > this.getMaxRows())) {
            throw new SQLException("Illegal value for setFetchSize()", "S1009");
        }

        fetchSize = rows;
    }

    /**
     * JDBC 2.0
     *
     * Determine the default fetch size.
     */
    public int getFetchSize()
                     throws SQLException {

        return fetchSize;
    }

    /**
     * DOCUMENT ME!
     * 
     * @return DOCUMENT ME! 
     * @throws SQLException DOCUMENT ME!
     */
    public java.sql.ResultSet getGeneratedKeys()
                                        throws SQLException {

        Field[] fields = new Field[1];
        fields[0] = new Field("", "GENERATED_KEY", Types.INTEGER, 17);

        ArrayList rowSet = new ArrayList();
        byte[][] row = new byte[1][];
        row[0] = Long.toString(getLastInsertID()).getBytes();
        rowSet.add(row);

        return new com.mysql.jdbc.ResultSet(fields, new RowDataStatic(rowSet), 
                                            connection);
    }

    /**
     * getLastInsertID returns the value of the auto_incremented key
     * after an executeQuery() or excute() call.
     *
     * <p>
     * This gets around the un-threadsafe behavior of
     * "select LAST_INSERT_ID()" which is tied to the Connection
     * that created this Statement, and therefore could have had
     * many INSERTS performed before one gets a chance to call
     * "select LAST_INSERT_ID()".
     *
     * @return the last update ID.
     */
    public long getLastInsertID() {

        if (Driver.TRACE) {

            Object[] args = new Object[0];
            Debug.methodCall(this, "getLastInsertID", args);
        }

        return lastInsertId;
    }

    /**
     * getLongUpdateCount returns the current result as an update count,
     * if the result is a ResultSet or there are no more results, -1
     * is returned.  It should only be called once per result.
     *
     * <p>
     * This method returns longs as MySQL server versions newer than 
     * 3.22.4 return 64-bit values for update counts
     *
     * @return the current result as an update count.
     * @exception java.sql.SQLException if a database access error occurs
     */
    public long getLongUpdateCount() {

        if (Driver.TRACE) {

            Object[] args = new Object[0];
            Debug.methodCall(this, "getLongUpdateCount", args);
        }

        if (results == null) {

            return -1;
        }

        if (results.reallyResult()) {

            return -1;
        }

        return updateCount;
    }

    /**
     * Sets the maxFieldSize
     *
     * @param max the new max column size limit; zero means unlimited
     * @exception java.sql.SQLException if size exceeds buffer size
     */
    public void setMaxFieldSize(int max)
                         throws java.sql.SQLException {

        if (Driver.TRACE) {

            Object[] args = { new Integer(max) };
            Debug.methodCall(this, "setMaxFieldSize", args);
        }

        if (max < 0) {
            throw new SQLException("Illegal value for setMaxFieldSize()", 
                                   "S1009");
        }

        int maxBuf = (connection != null)
                         ? connection.getMaxAllowedPacket() : MysqlIO.getMaxBuf();

        if (max > maxBuf) {
            throw new java.sql.SQLException("Can not set max field size > max allowed packet: "
                                            + maxBuf, "S1009");
        } else {
            maxFieldSize = max;
        }
    }

    /**
     * The maxFieldSize limit (in bytes) is the maximum amount of
     * data returned for any column value; it only applies to
     * BINARY, VARBINARY, LONGVARBINARY, CHAR, VARCHAR and LONGVARCHAR
     * columns.  If the limit is exceeded, the excess data is silently
     * discarded.
     *
     * @return the current max column size limit; zero means unlimited
     * @exception java.sql.SQLException if a database access error occurs
     */
    public int getMaxFieldSize()
                        throws java.sql.SQLException {

        if (Driver.TRACE) {

            Object[] args = new Object[0];
            Debug.methodCall(this, "getMaxFieldSize", args);
        }

        return maxFieldSize;
    }

    /**
     * Set the maximum number of rows
     *
     * @param max the new max rows limit; zero means unlimited
     * @exception java.sql.SQLException if a database access error occurs
     * @see getMaxRows
     */
    public void setMaxRows(int max)
                    throws java.sql.SQLException {

        if (Driver.TRACE) {

            Object[] args = { new Integer(max) };
            Debug.methodCall(this, "setMaxRows", args);
        }

        if (max > MysqlDefs.MAX_ROWS || max < 0) {
            throw new java.sql.SQLException("setMaxRows() out of range. "
                                            + max + " > " + MysqlDefs.MAX_ROWS
                                            + ".", "S1009");
        }

        if (max == 0) {
            max = -1;
        }

        maxRows = max;

        // Most people don't use setMaxRows()
        // so don't penalize them
        // with the extra query it takes
        // to do it efficiently unless we need
        // to.
        connection.maxRowsChanged();
    }

    /**
     * The maxRows limit is set to limit the number of rows that
     * any ResultSet can contain.  If the limit is exceeded, the
     * excess rows are silently dropped.
     *
     * @return the current maximum row limit; zero means unlimited
     * @exception java.sql.SQLException if a database access error occurs
     */
    public int getMaxRows()
                   throws java.sql.SQLException {

        if (Driver.TRACE) {

            Object[] args = new Object[0];
            Debug.methodCall(this, "getMaxRows", args);
        }

        if (maxRows <= 0) {

            return 0;
        } else {

            return maxRows;
        }
    }

    /**
     * getMoreResults moves to a Statement's next result.  If it returns
     * true, this result is a ResulSet.
     *
     * @return true if the next ResultSet is valid
     * @exception java.sql.SQLException if a database access error occurs
     */
    public boolean getMoreResults()
                           throws java.sql.SQLException {

        if (Driver.TRACE) {

            Object[] args = new Object[0];
            Debug.methodCall(this, "getMoreResults", args);
        }

        return getMoreResults(CLOSE_CURRENT_RESULT);
    }

    /**
     * @see Statement#getMoreResults(int)
     */
    public boolean getMoreResults(int current)
                           throws SQLException {

        switch (current) {

            case Statement.CLOSE_CURRENT_RESULT:
            case Statement.CLOSE_ALL_RESULTS:
            case Statement.KEEP_CURRENT_RESULT:

                if (results != null
                    && (current == CLOSE_ALL_RESULTS
                        || current == CLOSE_CURRENT_RESULT)) {
                    results.close();
                }

                results = nextResults;
                nextResults = null;

                return (results != null && results.reallyResult()) ? true : false;

            default:
                throw new SQLException("Illegal flag for getMoreResults(int)", 
                                       "S1009");
        }
    }

    /**
     * Sets the queryTimeout limit
     *
     * @param seconds - the new query timeout limit in seconds
     * @exception java.sql.SQLException if a database access error occurs
     */
    public void setQueryTimeout(int seconds)
                         throws java.sql.SQLException {

        if (Driver.TRACE) {

            Object[] args = { new Integer(seconds) };
            Debug.methodCall(this, "setQueryTimeout", args);
        }

        if (seconds < 0) {
            throw new SQLException("Illegal value for setQueryTimeout()", 
                                   "S1009");
        }

        timeout = seconds;
    }

    /**
     * The queryTimeout limit is the number of seconds the driver
     * will wait for a Statement to execute.  If the limit is
     * exceeded, a java.sql.SQLException is thrown.
     *
     * @return the current query timeout limit in seconds; 0 = unlimited
     * @exception java.sql.SQLException if a database access error occurs
     */
    public int getQueryTimeout()
                        throws java.sql.SQLException {

        if (Driver.TRACE) {

            Object[] args = new Object[0];
            Debug.methodCall(this, "getQueryTimeout", args);
        }

        return timeout;
    }

    /**
     * getResultSet returns the current result as a ResultSet.  It
     * should only be called once per result.
     *
     * @return the current result set; null if there are no more
     * @exception java.sql.SQLException if a database access error occurs (why?)
     */
    public synchronized java.sql.ResultSet getResultSet()
                                                 throws java.sql.SQLException {

        if (Driver.TRACE) {

            Object[] args = new Object[0];
            Debug.methodCall(this, "getResultSet", args);
        }

        return (results != null && results.reallyResult())
                   ? (java.sql.ResultSet) results : null;
    }

    /**
     * JDBC 2.0
     *
     * Determine the result set concurrency.
     */
    public int getResultSetConcurrency()
                                throws SQLException {

        return resultSetConcurrency;
    }

    /**
     * @see Statement#getResultSetHoldability()
     */
    public int getResultSetHoldability()
                                throws SQLException {

        return ResultSet.HOLD_CURSORS_OVER_COMMIT;
    }

    /**
     * JDBC 2.0
     *
     * Determine the result set type.
     */
    public int getResultSetType()
                         throws SQLException {

        return resultSetType;
    }

    /**
     * getUpdateCount returns the current result as an update count,
     * if the result is a ResultSet or there are no more results, -1
     * is returned.  It should only be called once per result.
     *
     * @return the current result as an update count.
     * @exception java.sql.SQLException if a database access error occurs
     */
    public synchronized int getUpdateCount()
                                    throws java.sql.SQLException {

        if (Driver.TRACE) {

            Object[] args = new Object[0];
            Debug.methodCall(this, "getUpdateCount", args);
        }

        if (results == null) {

            return -1;
        }

        if (results.reallyResult()) {

            return -1;
        }

        int truncatedUpdateCount = 0;

        if (results.getUpdateCount() > Integer.MAX_VALUE) {
            truncatedUpdateCount = Integer.MAX_VALUE;
        } else {
            truncatedUpdateCount = (int) results.getUpdateCount();
        }

        return truncatedUpdateCount;
    }

    /**
     * The first warning reported by calls on this Statement is
     * returned.  A Statement's execute methods clear its java.sql.SQLWarning
     * chain.  Subsequent Statement warnings will be chained to this
     * java.sql.SQLWarning.
     *
     * <p>The Warning chain is automatically cleared each time a statement
     * is (re)executed.
     *
     * <p><B>Note:</B>  If you are processing a ResultSet then any warnings
     * associated with ResultSet reads will be chained on the ResultSet
     * object.
     *
     * @return the first java.sql.SQLWarning on null
     * @exception java.sql.SQLException if a database access error occurs
     */
    public synchronized java.sql.SQLWarning getWarnings()
                                                 throws java.sql.SQLException {

        if (Driver.TRACE) {

            Object[] args = new Object[0];
            Debug.methodCall(this, "getWarnings", args);
        }

        return warningChain;
    }

    /**
     * DOCUMENT ME!
     * 
     * @param sql DOCUMENT ME!
     * @throws SQLException DOCUMENT ME!
     */
    public synchronized void addBatch(String sql)
                               throws SQLException {

        if (batchedArgs == null) {
            batchedArgs = new ArrayList();
        }

        if (sql != null) {
            batchedArgs.add(sql);
        }
    }

    /**
     * Cancel can be used by one thread to cancel a statement that
     * is being executed by another thread.  However this driver
     * is synchronous, so this really has no meaning - we
     * define it as a no-op (i.e. you can't cancel, but there is no
     * error if you try.)
     *
     * @exception java.sql.SQLException only because thats the spec.
     */
    public void cancel()
                throws java.sql.SQLException {

        if (Driver.TRACE) {

            Object[] args = new Object[0];
            Debug.methodCall(this, "cancel", args);
        }

        // No-op
    }

    /**
     * JDBC 2.0
     *
     * Make the set of commands in the current batch empty.
     * This method is optional.
     *
     * @exception SQLException if a database-access error occurs, or the
     * driver does not support batch statements
     */
    public synchronized void clearBatch()
                                 throws SQLException {

        if (batchedArgs != null) {
            batchedArgs.clear();
        }
    }

    /**
     * After this call, getWarnings returns null until a new warning
     * is reported for this Statement.
     *
     * @exception java.sql.SQLException if a database access error occurs (why?)
     */
    public synchronized void clearWarnings()
                                    throws java.sql.SQLException {

        if (Driver.TRACE) {

            Object[] args = new Object[0];
            Debug.methodCall(this, "clearWarnings", args);
        }

        warningChain = null;
    }

    /**
     * In many cases, it is desirable to immediately release a
     * Statement's database and JDBC resources instead of waiting
     * for this to happen when it is automatically closed.  The
     * close method provides this immediate release.
     *
     * <p><B>Note:</B> A Statement is automatically closed when it is
     * garbage collected.  When a Statement is closed, its current
     * ResultSet, if one exists, is also closed.
     *
     * @exception java.sql.SQLException if a database access error occurs
     */
    public synchronized void close()
                            throws java.sql.SQLException {

        if (Driver.TRACE) {

            Object[] args = new Object[0];
            Debug.methodCall(this, "close", args);
        }

        if (results != null) {

            try {
                results.close();
            } catch (Exception ex) {
                ;
            }
        }

        results = null;
        connection = null;
        warningChain = null;
        escaper = null;
        isClosed = true;
    }

    /**
     * Execute a SQL statement that may return multiple results. We
     * don't have to worry about this since we do not support multiple
     * ResultSets.   You can use getResultSet or getUpdateCount to
     * retrieve the result.
     *
     * @param sql any SQL statement
     * @return true if the next result is a ResulSet, false if it is
     *      an update count or there are no more results
     * @exception java.sql.SQLException if a database access error occurs
     */
    public synchronized boolean execute(String sql)
                                 throws java.sql.SQLException {

        if (Driver.TRACE) {

            Object[] args = { sql };
            Debug.methodCall(this, "execute", args);
        }

        if (connection.isReadOnly()) {

            if (sql != null) {

                int length = sql.length();
                char firstNonWsChar = 0;

                for (int i = 0; i < length; i++) {

                    char c = sql.charAt(i);

                    if (!Character.isWhitespace(c)) {
                        firstNonWsChar = c;

                        break;
                    }
                }

                if (firstNonWsChar != 'S' && firstNonWsChar != 's') {
                    throw new SQLException("Connection is read-only. "
                                           + "Queries leading to data modification are not allowed", 
                                           "S1009");
                }
            }
        }

        checkClosed();

        if (doEscapeProcessing) {
            sql = escaper.escapeSQL(sql);
        }

        if (results != null) {
            results.close();
        }

        ResultSet rs = null;

        // If there isn't a limit clause in the SQL
        // then limit the number of rows to return in
        // an efficient manner. Only do this if
        // setMaxRows() hasn't been used on any Statements
        // generated from the current Connection (saves
        // a query, and network traffic).
        synchronized (connection.getMutex()) {

            String oldCatalog = null;

            if (!connection.getCatalog().equals(currentCatalog)) {
                oldCatalog = connection.getCatalog();
                connection.setCatalog(currentCatalog);
            }

            char firstChar = Character.toUpperCase(sql.charAt(0));
                
            boolean isSelect = (firstChar == 'S');
            
            //
            // Only apply max_rows to selects
            //
            if (connection.useMaxRows()) {

               
                
                if (isSelect) {

                    if (sql.toUpperCase().indexOf("LIMIT") != -1) {
                        rs = connection.execSQL(sql, maxRows, 
                                                resultSetConcurrency, 
                                                createStreamingResultSet(), true);
                    } else {

                        if (maxRows <= 0) {
                            connection.execSQL(
                                    "SET OPTION SQL_SELECT_LIMIT=DEFAULT", -1);
                        } else {
                            connection.execSQL(
                                    "SET OPTION SQL_SELECT_LIMIT=" + maxRows, 
                                    -1);
                        }
                    }
                } else {
                    connection.execSQL("SET OPTION SQL_SELECT_LIMIT=DEFAULT", 
                                       -1);
                }

                // Finally, execute the query
                rs = connection.execSQL(sql, -1, resultSetConcurrency, 
                                        createStreamingResultSet(), isSelect);
            } else {
                rs = connection.execSQL(sql, -1, resultSetConcurrency, 
                                        createStreamingResultSet(), isSelect);
            }

            if (oldCatalog != null) {
                connection.setCatalog(oldCatalog);
            }
        }

        lastInsertId = rs.getUpdateID();

        if (rs != null) {
            results = rs;
        }

        rs.setConnection(connection);
        rs.setResultSetType(resultSetType);
        rs.setResultSetConcurrency(resultSetConcurrency);

        return (rs != null && rs.reallyResult());
    }

    /**
     * @see Statement#execute(String, int)
     */
    public boolean execute(String arg0, int arg1)
                    throws SQLException {

        return execute(arg0);
    }

    /**
     * @see Statement#execute(String, int[])
     */
    public boolean execute(String arg0, int[] arg1)
                    throws SQLException {

        return execute(arg0);
    }

    /**
     * @see Statement#execute(String, String[])
     */
    public boolean execute(String arg0, String[] arg1)
                    throws SQLException {

        return execute(arg0);
    }

    /**
    * JDBC 2.0
    * 
    * Submit a batch of commands to the database for execution.
    * This method is optional.
    *
    * @return an array of update counts containing one element for each
    * command in the batch.  The array is ordered according 
    * to the order in which commands were inserted into the batch
    * @exception SQLException if a database-access error occurs, or the
    * driver does not support batch statements
    */
    public synchronized int[] executeBatch()
                                    throws SQLException {

        if (connection.isReadOnly()) {
            throw new SQLException("Connection is read-only. "
                                   + "Queries leading to data modification are not allowed", 
                                   "S1009");
        }

        try {

            int[] updateCounts = null;

            if (batchedArgs != null) {

                int nbrCommands = batchedArgs.size();
                updateCounts = new int[nbrCommands];

                for (int i = 0; i < nbrCommands; i++) {
                    updateCounts[i] = -3;
                }

                SQLException sqlEx = null;

                for (int i = 0; i < nbrCommands; i++) {

                    try {
                        updateCounts[i] = executeUpdate(
                                                  (String) batchedArgs.get(i));
                    } catch (SQLException ex) {
                        sqlEx = ex;
                    }
                }

                if (sqlEx != null) {
                    throw new java.sql.BatchUpdateException(sqlEx.getMessage(), 
                                                            sqlEx.getSQLState(), 
                                                            sqlEx.getErrorCode(), 
                                                            updateCounts);
                }
            }

            return updateCounts != null ? updateCounts : new int[0];
        } finally {
            clearBatch();
        }
    }

    /**
     * Execute a SQL statement that retruns a single ResultSet
     *
     * @param Sql typically a static SQL SELECT statement
     * @return a ResulSet that contains the data produced by the query
     * @exception java.sql.SQLException if a database access error occurs
     */
    public synchronized java.sql.ResultSet executeQuery(String sql)
                                                 throws java.sql.SQLException {

        if (Driver.TRACE) {

            Object[] args = { sql };
            Debug.methodCall(this, "executeQuery", args);
        }

        checkClosed();

        if (doEscapeProcessing) {
            sql = escaper.escapeSQL(sql);
        }

        if (results != null) {
            results.close();
        }

        // If there isn't a limit clause in the SQL
        // then limit the number of rows to return in
        // an efficient manner. Only do this if
        // setMaxRows() hasn't been used on any Statements
        // generated from the current Connection (saves
        // a query, and network traffic).
        synchronized (connection.getMutex()) {

            String oldCatalog = null;

            if (!connection.getCatalog().equals(currentCatalog)) {
                oldCatalog = connection.getCatalog();
                connection.setCatalog(currentCatalog);
            }

            if (connection.useMaxRows()) {

                // We need to execute this all together
                // So synchronize on the Connection's mutex (because
                // even queries going through there synchronize
                // on the connection
                if (sql.toUpperCase().indexOf("LIMIT") != -1) {
                    results = connection.execSQL(sql, maxRows, 
                                                 resultSetConcurrency, 
                                                 createStreamingResultSet(), true);
                } else {

                    if (maxRows <= 0) {
                        connection.execSQL(
                                "SET OPTION SQL_SELECT_LIMIT=DEFAULT", -1);
                    } else {
                        connection.execSQL(
                                "SET OPTION SQL_SELECT_LIMIT=" + maxRows, -1);
                    }

                    results = connection.execSQL(sql, -1, resultSetConcurrency, 
                                                 createStreamingResultSet(), true);

                    if (oldCatalog != null) {
                        connection.setCatalog(oldCatalog);
                    }
                }
            } else {
                results = connection.execSQL(sql, -1, resultSetConcurrency, 
                                             createStreamingResultSet(), true);
            }

            if (oldCatalog != null) {
                connection.setCatalog(oldCatalog);
            }
        }

        lastInsertId = results.getUpdateID();
        nextResults = results;
        results.setConnection(connection);
        results.setResultSetType(resultSetType);
        results.setResultSetConcurrency(resultSetConcurrency);
        results.setStatement(this);

        if (!results.reallyResult()) {

            if (!connection.getAutoCommit()) {

                try {
                    connection.rollback();
                } catch (SQLException sqlEx) {

                    // FIXME: Log later?
                }
            }

            throw new SQLException("Can not issue INSERT/UPDATE/DELETE with executeQuery()", 
                                   "S1009");
        }

        return (java.sql.ResultSet) results;
    }

    /**
     * Execute a SQL INSERT, UPDATE or DELETE statement.  In addition
     * SQL statements that return nothing such as SQL DDL statements
     * can be executed
     *
     * Any IDs generated for AUTO_INCREMENT fields can be retrieved
     * by casting this Statement to org.gjt.mm.mysql.Statement and
     * calling the getLastInsertID() method.
     *
     * @param Sql a SQL statement
     * @return either a row count, or 0 for SQL commands
     * @exception java.sql.SQLException if a database access error occurs
     */
    public synchronized int executeUpdate(String sql)
                                   throws java.sql.SQLException {

        if (Driver.TRACE) {

            Object[] args = { sql };
            Debug.methodCall(this, "executeUpdate", args);
        }

        if (connection.isReadOnly()) {
            throw new SQLException("Connection is read-only. "
                                   + "Queries leading to data modification are not allowed", 
                                   "S1009");
        }

        checkClosed();

        if (doEscapeProcessing) {
            sql = escaper.escapeSQL(sql);
        }

        // The checking and changing of catalogs
        // must happen in sequence, so synchronize
        // on the same mutex that _conn is using
        ResultSet rs = null;

        synchronized (connection.getMutex()) {

            String oldCatalog = null;

            if (!connection.getCatalog().equals(currentCatalog)) {
                oldCatalog = connection.getCatalog();
                connection.setCatalog(currentCatalog);
            }

            //
            // Only apply max_rows to selects
            //
            if (connection.useMaxRows()) {
                connection.execSQL("SET OPTION SQL_SELECT_LIMIT=DEFAULT", -1);
            }

            rs = connection.execSQL(sql, -1, 
                                    java.sql.ResultSet.CONCUR_READ_ONLY, false, false);
            rs.setConnection(connection);

            if (oldCatalog != null) {
                connection.setCatalog(oldCatalog);
            }
        }

        if (rs.reallyResult()) {

            if (!connection.getAutoCommit()) {

                try {
                    connection.rollback();
                } catch (SQLException sqlEx) {

                    // FIXME: Log later?
                }
            }

            rs.close();
            throw new java.sql.SQLException("Results returned for UPDATE ONLY.", 
                                            "01S03");
        } else {
            updateCount = rs.getUpdateCount();

            int truncatedUpdateCount = 0;

            if (updateCount > Integer.MAX_VALUE) {
                truncatedUpdateCount = Integer.MAX_VALUE;
            } else {
                truncatedUpdateCount = (int) updateCount;
            }

            lastInsertId = rs.getUpdateID();

            return truncatedUpdateCount;
        }
    }

    /**
     * @see Statement#executeUpdate(String, int)
     */
    public int executeUpdate(String arg0, int arg1)
                      throws SQLException {

        return executeUpdate(arg0);
    }

    /**
     * @see Statement#executeUpdate(String, int[])
     */
    public int executeUpdate(String arg0, int[] arg1)
                      throws SQLException {

        return executeUpdate(arg0);
    }

    /**
     * @see Statement#executeUpdate(String, String[])
     */
    public int executeUpdate(String arg0, String[] arg1)
                      throws SQLException {

        return executeUpdate(arg0);
    }

    protected void checkClosed()
                        throws SQLException {

        if (isClosed) {
            throw new SQLException("No operations allowed after statement closed");
        }
    }

    /**
     * Sets the concurrency for result sets generated by this statement
     */
    void setResultSetConcurrency(int concurrencyFlag) {
        resultSetConcurrency = concurrencyFlag;
    }

    /**
     * Sets the result set type for result sets generated by this statement
     */
    void setResultSetType(int typeFlag) {
        resultSetType = typeFlag;
    }

    /**
     * We only stream result sets when they are forward-only, read-only,
     * and the fetch size has been set to Integer.MIN_VALUE
     */
    protected boolean createStreamingResultSet() {

        if (!(resultSetType == java.sql.ResultSet.TYPE_FORWARD_ONLY
                && resultSetConcurrency == java.sql.ResultSet.CONCUR_READ_ONLY
                && fetchSize == Integer.MIN_VALUE)) {

            return false;
        } else {

            return true;
        }
    }
    
    
}