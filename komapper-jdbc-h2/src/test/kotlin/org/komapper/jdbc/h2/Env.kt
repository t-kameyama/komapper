@file:Suppress("unused")

package org.komapper.jdbc.h2

import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.komapper.core.Database
import org.komapper.core.KmColumn
import org.komapper.core.KmCreatedAt
import org.komapper.core.KmEntity
import org.komapper.core.KmId
import org.komapper.core.KmIdentityGenerator
import org.komapper.core.KmIgnore
import org.komapper.core.KmSequenceGenerator
import org.komapper.core.KmTable
import org.komapper.core.KmUpdatedAt
import org.komapper.core.KmVersion
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

@KmEntity
data class Address(
    @KmId @KmColumn(name = "ADDRESS_ID") val addressId: Int,
    val street: String,
    @KmVersion val version: Int
) {
    companion object
}

data class CompositeKeyAddress(
    val addressId1: Int,
    val addressId2: Int,
    val street: String,
    val version: Int
)

@KmEntity
@KmTable("IDENTITY_STRATEGY")
data class IdentityStrategy(
    @KmId @KmIdentityGenerator val id: Int?,
    val value: String
) {
    companion object
}

@KmEntity
@KmTable("SEQUENCE_STRATEGY")
data class SequenceStrategy(
    @KmId @KmSequenceGenerator(name = "SEQUENCE_STRATEGY_ID", incrementBy = 100) val id: Int,
    val value: String
) {
    companion object
}

@KmEntity
data class Person(
    @KmId @KmColumn("PERSON_ID") val personId: Int,
    val name: String,
    @KmCreatedAt @KmColumn("CREATED_AT") val createdAt: LocalDateTime? = null,
    @KmUpdatedAt @KmColumn("UPDATED_AT") val updatedAt: LocalDateTime? = null
) {
    companion object
}

@KmEntity @KmTable("PERSON")
data class Human(
    @KmId @KmColumn("PERSON_ID") val humanId: Int,
    val name: String,
    @KmCreatedAt val createdAt: OffsetDateTime? = null,
    @KmUpdatedAt val updatedAt: OffsetDateTime? = null
) {
    companion object
}

@KmEntity
data class Employee(
    @KmId @KmColumn("EMPLOYEE_ID") val employeeId: Int,
    @KmColumn("EMPLOYEE_NO") val employeeNo: Int,
    @KmColumn("EMPLOYEE_NAME") val employeeName: String,
    @KmColumn("MANAGER_ID") val managerId: Int?,
    val hiredate: LocalDate,
    val salary: BigDecimal,
    @KmColumn("DEPARTMENT_ID") val departmentId: Int,
    @KmColumn("ADDRESS_ID") val addressId: Int,
    @KmVersion val version: Int,
    @KmIgnore val address: Address? = null,
    @KmIgnore val department: Department? = null
) {
    companion object
}

data class WorkerSalary(val salary: BigDecimal)

data class WorkerDetail(
    val hiredate: LocalDate,
    val salary: WorkerSalary
)

data class Worker(
    val employeeId: Int,
    val employeeNo: Int,
    val employeeName: String,
    val managerId: Int?,
    val detail: WorkerDetail,
    val departmentId: Int,
    val addressId: Int,
    val version: Int
)

data class Common(
    val personId: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.of(2000, 1, 1, 0, 0, 0),
    val updatedAt: LocalDateTime = LocalDateTime.of(2000, 1, 1, 0, 0, 0),
    val version: Int = 0
)

@KmEntity
data class Department(
    @KmId @KmColumn("DEPARTMENT_ID") val departmentId: Int,
    @KmColumn("DEPARTMENT_NO") val departmentNo: Int,
    @KmColumn("DEPARTMENT_NAME") val departmentName: String,
    val location: String,
    @KmVersion val version: Int,
    @KmIgnore val employeeList: List<Employee> = emptyList()
) {
    companion object
}

data class NoId(val value1: Int, val value2: Int)

internal class Env :
    BeforeTestExecutionCallback,
    AfterTestExecutionCallback,
    ParameterResolver {

    private val config = object : H2DatabaseConfig("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", enableTransaction = true) {
        override val jdbcOption = super.jdbcOption.copy(batchSize = 2)
    }

    private val db = Database(config)
    private val txManager = db.config.session.getTransactionManager()

    override fun beforeTestExecution(context: ExtensionContext?) {
        db.transaction {
            db.script(
                """
            CREATE SEQUENCE SEQUENCE_STRATEGY_ID START WITH 1 INCREMENT BY 100;
            CREATE SEQUENCE MY_SEQUENCE_STRATEGY_ID START WITH 1 INCREMENT BY 100;
            CREATE SEQUENCE PERSON_ID_SEQUENCE START WITH 1 INCREMENT BY 100;

            CREATE TABLE DEPARTMENT(DEPARTMENT_ID INTEGER NOT NULL PRIMARY KEY, DEPARTMENT_NO INTEGER NOT NULL UNIQUE,DEPARTMENT_NAME VARCHAR(20),LOCATION VARCHAR(20) DEFAULT 'TOKYO', VERSION INTEGER);
            CREATE TABLE ADDRESS(ADDRESS_ID INTEGER NOT NULL PRIMARY KEY, STREET VARCHAR(20) UNIQUE, VERSION INTEGER);
            CREATE TABLE EMPLOYEE(EMPLOYEE_ID INTEGER NOT NULL PRIMARY KEY, EMPLOYEE_NO INTEGER NOT NULL ,EMPLOYEE_NAME VARCHAR(20),MANAGER_ID INTEGER,HIREDATE DATE,SALARY NUMERIC(7,2),DEPARTMENT_ID INTEGER,ADDRESS_ID INTEGER,VERSION INTEGER, CONSTRAINT FK_DEPARTMENT_ID FOREIGN KEY(DEPARTMENT_ID) REFERENCES DEPARTMENT(DEPARTMENT_ID), CONSTRAINT FK_ADDRESS_ID FOREIGN KEY(ADDRESS_ID) REFERENCES ADDRESS(ADDRESS_ID));
            CREATE TABLE PERSON(PERSON_ID INTEGER NOT NULL PRIMARY KEY, NAME VARCHAR(20), CREATED_AT TIMESTAMP, UPDATED_AT TIMESTAMP, VERSION INTEGER);

            CREATE TABLE COMP_KEY_DEPARTMENT(DEPARTMENT_ID1 INTEGER NOT NULL, DEPARTMENT_ID2 INTEGER NOT NULL, DEPARTMENT_NO INTEGER NOT NULL UNIQUE,DEPARTMENT_NAME VARCHAR(20),LOCATION VARCHAR(20) DEFAULT 'TOKYO', VERSION INTEGER, CONSTRAINT PK_COMP_KEY_DEPARTMENT PRIMARY KEY(DEPARTMENT_ID1, DEPARTMENT_ID2));
            CREATE TABLE COMP_KEY_ADDRESS(ADDRESS_ID1 INTEGER NOT NULL, ADDRESS_ID2 INTEGER NOT NULL, STREET VARCHAR(20), VERSION INTEGER, CONSTRAINT PK_COMP_KEY_ADDRESS PRIMARY KEY(ADDRESS_ID1, ADDRESS_ID2));
            CREATE TABLE COMP_KEY_EMPLOYEE(EMPLOYEE_ID1 INTEGER NOT NULL, EMPLOYEE_ID2 INTEGER NOT NULL, EMPLOYEE_NO INTEGER NOT NULL ,EMPLOYEE_NAME VARCHAR(20),MANAGER_ID1 INTEGER,MANAGER_ID2 INTEGER,HIREDATE DATE,SALARY NUMERIC(7,2),DEPARTMENT_ID1 INTEGER,DEPARTMENT_ID2 INTEGER,ADDRESS_ID1 INTEGER,ADDRESS_ID2 INTEGER,VERSION INTEGER, CONSTRAINT PK_COMP_KEY_EMPLOYEE PRIMARY KEY(EMPLOYEE_ID1, EMPLOYEE_ID2), CONSTRAINT FK_COMP_KEY_DEPARTMENT_ID FOREIGN KEY(DEPARTMENT_ID1, DEPARTMENT_ID2) REFERENCES COMP_KEY_DEPARTMENT(DEPARTMENT_ID1, DEPARTMENT_ID2), CONSTRAINT FK_COMP_KEY_ADDRESS_ID FOREIGN KEY(ADDRESS_ID1, ADDRESS_ID2) REFERENCES COMP_KEY_ADDRESS(ADDRESS_ID1, ADDRESS_ID2));

            CREATE TABLE LARGE_OBJECT(ID NUMERIC(8) NOT NULL PRIMARY KEY, NAME VARCHAR(20), LARGE_NAME CLOB, BYTES BINARY, LARGE_BYTES BLOB, DTO BINARY, LARGE_DTO BLOB);
            CREATE TABLE TENSE (ID INTEGER PRIMARY KEY,DATE_DATE DATE, DATE_TIME TIME, DATE_TIMESTAMP TIMESTAMP, CAL_DATE DATE, CAL_TIME TIME, CAL_TIMESTAMP TIMESTAMP, SQL_DATE DATE, SQL_TIME TIME, SQL_TIMESTAMP TIMESTAMP);
            CREATE TABLE JOB (ID INTEGER NOT NULL PRIMARY KEY, JOB_TYPE VARCHAR(20));
            CREATE TABLE AUTHORITY (ID INTEGER NOT NULL PRIMARY KEY, AUTHORITY_TYPE INTEGER);
            CREATE TABLE NO_ID (VALUE1 INTEGER, VALUE2 INTEGER);
            CREATE TABLE OWNER_OF_NO_ID (ID INTEGER NOT NULL PRIMARY KEY, NO_ID_VALUE1 INTEGER);
            CREATE TABLE CONSTRAINT_CHECKING (PRIMARY_KEY INTEGER PRIMARY KEY, UNIQUE_KEY INTEGER UNIQUE, FOREIGN_KEY INTEGER, CHECK_CONSTRAINT INTEGER, NOT_NULL INTEGER NOT NULL, CONSTRAINT CK_CONSTRAINT_CHECKING_1 CHECK (CHECK_CONSTRAINT > 0), CONSTRAINT FK_JOB_ID FOREIGN KEY (FOREIGN_KEY) REFERENCES JOB (ID));
            CREATE TABLE PATTERN (VALUE VARCHAR(10));

            CREATE TABLE ID_GENERATOR(PK VARCHAR(20) NOT NULL PRIMARY KEY, VALUE INTEGER NOT NULL);
            CREATE TABLE MY_ID_GENERATOR(MY_PK VARCHAR(20) NOT NULL PRIMARY KEY, MY_VALUE INTEGER NOT NULL);
            CREATE TABLE AUTO_STRATEGY(ID INTEGER NOT NULL IDENTITY PRIMARY KEY, VALUE VARCHAR(10));
            CREATE TABLE IDENTITY_STRATEGY(ID INTEGER NOT NULL IDENTITY PRIMARY KEY, VALUE VARCHAR(10));
            CREATE TABLE SEQUENCE_STRATEGY(ID INTEGER NOT NULL PRIMARY KEY, VALUE VARCHAR(10));
            CREATE TABLE SEQUENCE_STRATEGY2(ID INTEGER NOT NULL PRIMARY KEY, VALUE VARCHAR(10));
            CREATE TABLE TABLE_STRATEGY(ID INTEGER NOT NULL PRIMARY KEY, VALUE VARCHAR(10));
            CREATE TABLE TABLE_STRATEGY2(ID INTEGER NOT NULL PRIMARY KEY, VALUE VARCHAR(10));

            CREATE TABLE ANY_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE OTHER);
            CREATE TABLE BIG_DECIMAL_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE BIGINT);
            CREATE TABLE BIG_INTEGER_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE BIGINT);
            CREATE TABLE BOOLEAN_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE BOOL);
            CREATE TABLE BYTE_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE TINYINT);
            CREATE TABLE BYTE_ARRAY_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE BINARY);
            CREATE TABLE DOUBLE_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE DOUBLE);
            CREATE TABLE ENUM_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE VARCHAR(20));
            CREATE TABLE FLOAT_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE FLOAT);
            CREATE TABLE INT_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE INTEGER);
            CREATE TABLE LOCAL_DATE_TIME_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE TIMESTAMP);
            CREATE TABLE LOCAL_DATE_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE DATE);
            CREATE TABLE LOCAL_TIME_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE TIME);
            CREATE TABLE LONG_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE BIGINT);
            CREATE TABLE OFFSET_DATE_TIME_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE TIMESTAMP WITH TIME ZONE);
            CREATE TABLE SHORT_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE SMALLINT);
            CREATE TABLE STRING_TEST(ID INTEGER NOT NULL PRIMARY KEY, VALUE VARCHAR(20));

            INSERT INTO DEPARTMENT VALUES(1,10,'ACCOUNTING','NEW YORK',1);
            INSERT INTO DEPARTMENT VALUES(2,20,'RESEARCH','DALLAS',1);
            INSERT INTO DEPARTMENT VALUES(3,30,'SALES','CHICAGO',1);
            INSERT INTO DEPARTMENT VALUES(4,40,'OPERATIONS','BOSTON',1);
            INSERT INTO ADDRESS VALUES(1,'STREET 1',1);
            INSERT INTO ADDRESS VALUES(2,'STREET 2',1);
            INSERT INTO ADDRESS VALUES(3,'STREET 3',1);
            INSERT INTO ADDRESS VALUES(4,'STREET 4',1);
            INSERT INTO ADDRESS VALUES(5,'STREET 5',1);
            INSERT INTO ADDRESS VALUES(6,'STREET 6',1);
            INSERT INTO ADDRESS VALUES(7,'STREET 7',1);
            INSERT INTO ADDRESS VALUES(8,'STREET 8',1);
            INSERT INTO ADDRESS VALUES(9,'STREET 9',1);
            INSERT INTO ADDRESS VALUES(10,'STREET 10',1);
            INSERT INTO ADDRESS VALUES(11,'STREET 11',1);
            INSERT INTO ADDRESS VALUES(12,'STREET 12',1);
            INSERT INTO ADDRESS VALUES(13,'STREET 13',1);
            INSERT INTO ADDRESS VALUES(14,'STREET 14',1);
            INSERT INTO ADDRESS VALUES(15,'STREET 15',1);
            INSERT INTO EMPLOYEE VALUES(1,7369,'SMITH',13,'1980-12-17',800,2,1,1);
            INSERT INTO EMPLOYEE VALUES(2,7499,'ALLEN',6,'1981-02-20',1600,3,2,1);
            INSERT INTO EMPLOYEE VALUES(3,7521,'WARD',6,'1981-02-22',1250,3,3,1);
            INSERT INTO EMPLOYEE VALUES(4,7566,'JONES',9,'1981-04-02',2975,2,4,1);
            INSERT INTO EMPLOYEE VALUES(5,7654,'MARTIN',6,'1981-09-28',1250,3,5,1);
            INSERT INTO EMPLOYEE VALUES(6,7698,'BLAKE',9,'1981-05-01',2850,3,6,1);
            INSERT INTO EMPLOYEE VALUES(7,7782,'CLARK',9,'1981-06-09',2450,1,7,1);
            INSERT INTO EMPLOYEE VALUES(8,7788,'SCOTT',4,'1982-12-09',3000.0,2,8,1);
            INSERT INTO EMPLOYEE VALUES(9,7839,'KING',NULL,'1981-11-17',5000,1,9,1);
            INSERT INTO EMPLOYEE VALUES(10,7844,'TURNER',6,'1981-09-08',1500,3,10,1);
            INSERT INTO EMPLOYEE VALUES(11,7876,'ADAMS',8,'1983-01-12',1100,2,11,1);
            INSERT INTO EMPLOYEE VALUES(12,7900,'JAMES',6,'1981-12-03',950,3,12,1);
            INSERT INTO EMPLOYEE VALUES(13,7902,'FORD',4,'1981-12-03',3000,2,13,1);
            INSERT INTO EMPLOYEE VALUES(14,7934,'MILLER',7,'1982-01-23',1300,1,14,1);

            INSERT INTO COMP_KEY_DEPARTMENT VALUES(1,1,10,'ACCOUNTING','NEW YORK',1);
            INSERT INTO COMP_KEY_DEPARTMENT VALUES(2,2,20,'RESEARCH','DALLAS',1);
            INSERT INTO COMP_KEY_DEPARTMENT VALUES(3,3,30,'SALES','CHICAGO',1);
            INSERT INTO COMP_KEY_DEPARTMENT VALUES(4,4,40,'OPERATIONS','BOSTON',1);
            INSERT INTO COMP_KEY_ADDRESS VALUES(1,1,'STREET 1',1);
            INSERT INTO COMP_KEY_ADDRESS VALUES(2,2,'STREET 2',1);
            INSERT INTO COMP_KEY_ADDRESS VALUES(3,3,'STREET 3',1);
            INSERT INTO COMP_KEY_ADDRESS VALUES(4,4,'STREET 4',1);
            INSERT INTO COMP_KEY_ADDRESS VALUES(5,5,'STREET 5',1);
            INSERT INTO COMP_KEY_ADDRESS VALUES(6,6,'STREET 6',1);
            INSERT INTO COMP_KEY_ADDRESS VALUES(7,7,'STREET 7',1);
            INSERT INTO COMP_KEY_ADDRESS VALUES(8,8,'STREET 8',1);
            INSERT INTO COMP_KEY_ADDRESS VALUES(9,9,'STREET 9',1);
            INSERT INTO COMP_KEY_ADDRESS VALUES(10,10,'STREET 10',1);
            INSERT INTO COMP_KEY_ADDRESS VALUES(11,11,'STREET 11',1);
            INSERT INTO COMP_KEY_ADDRESS VALUES(12,12,'STREET 12',1);
            INSERT INTO COMP_KEY_ADDRESS VALUES(13,13,'STREET 13',1);
            INSERT INTO COMP_KEY_ADDRESS VALUES(14,14,'STREET 14',1);
            INSERT INTO COMP_KEY_EMPLOYEE VALUES(1,1,7369,'SMITH',13,13,'1980-12-17',800,2,2,1,1,1);
            INSERT INTO COMP_KEY_EMPLOYEE VALUES(2,2,7499,'ALLEN',6,6,'1981-02-20',1600,3,3,2,2,1);
            INSERT INTO COMP_KEY_EMPLOYEE VALUES(3,3,7521,'WARD',6,6,'1981-02-22',1250,3,3,3,3,1);
            INSERT INTO COMP_KEY_EMPLOYEE VALUES(4,4,7566,'JONES',9,9,'1981-04-02',2975,2,2,4,4,1);
            INSERT INTO COMP_KEY_EMPLOYEE VALUES(5,5,7654,'MARTIN',6,6,'1981-09-28',1250,3,3,5,5,1);
            INSERT INTO COMP_KEY_EMPLOYEE VALUES(6,6,7698,'BLAKE',9,9,'1981-05-01',2850,3,3,6,6,1);
            INSERT INTO COMP_KEY_EMPLOYEE VALUES(7,7,7782,'CLARK',9,9,'1981-06-09',2450,1,1,7,7,1);
            INSERT INTO COMP_KEY_EMPLOYEE VALUES(8,8,7788,'SCOTT',4,4,'1982-12-09',3000.0,2,2,8,8,1);
            INSERT INTO COMP_KEY_EMPLOYEE VALUES(9,9,7839,'KING',NULL,NULL,'1981-11-17',5000,1,1,9,9,1);
            INSERT INTO COMP_KEY_EMPLOYEE VALUES(10,10,7844,'TURNER',6,6,'1981-09-08',1500,3,3,10,10,1);
            INSERT INTO COMP_KEY_EMPLOYEE VALUES(11,11,7876,'ADAMS',8,8,'1983-01-12',1100,2,2,11,11,1);
            INSERT INTO COMP_KEY_EMPLOYEE VALUES(12,12,7900,'JAMES',6,6,'1981-12-03',950,3,3,12,12,1);
            INSERT INTO COMP_KEY_EMPLOYEE VALUES(13,13,7902,'FORD',4,4,'1981-12-03',3000,2,2,13,13,1);
            INSERT INTO COMP_KEY_EMPLOYEE VALUES(14,14,7934,'MILLER',7,7,'1982-01-23',1300,1,1,14,14,1);

            INSERT INTO TENSE VALUES (1, '2005-02-14', '12:11:10', '2005-02-14 12:11:10', '2005-02-14', '12:11:10', '2005-02-14 12:11:10', '2005-02-14', '12:11:10', '2005-02-14 12:11:10');
            INSERT INTO JOB VALUES (1, 'SALESMAN');
            INSERT INTO JOB VALUES (2, 'MANAGER');
            INSERT INTO JOB VALUES (3, 'PRESIDENT');
            INSERT INTO AUTHORITY VALUES (1, 10);
            INSERT INTO AUTHORITY VALUES (2, 20);
            INSERT INTO AUTHORITY VALUES (3, 30);
            INSERT INTO NO_ID VALUES (1, 1);
            INSERT INTO NO_ID VALUES (1, 1);

            INSERT INTO ID_GENERATOR VALUES('TABLE_STRATEGY_ID', 1);
                """.trimIndent()
            )
        }
        txManager.begin()
    }

    override fun afterTestExecution(context: ExtensionContext?) {
        txManager.rollback()
        db.transaction {
            db.config.session.getConnection().use { con ->
                con.createStatement().use { stmt ->
                    stmt.execute("DROP ALL OBJECTS")
                }
            }
        }
    }

    override fun supportsParameter(
        parameterContext: ParameterContext?,
        extensionContext: ExtensionContext?
    ): Boolean =
        parameterContext!!.parameter.type === Database::class.java

    override fun resolveParameter(parameterContext: ParameterContext?, extensionContext: ExtensionContext?): Any =
        db
}
