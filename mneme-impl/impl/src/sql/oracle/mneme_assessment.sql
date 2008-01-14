--*********************************************************************************
-- $URL$
-- $Id$
--**********************************************************************************
--
-- Copyright (c) 2007, 2008 The Regents of the University of Michigan & Foothill College, ETUDES Project
-- 
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
-- 
--      http://www.apache.org/licenses/LICENSE-2.0
-- 
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--
--*********************************************************************************/

-----------------------------------------------------------------------------
-- Mneme Assessment DDL
-----------------------------------------------------------------------------

CREATE TABLE MNEME_ASSESSMENT
(
	ARCHIVED				CHAR (1) NOT NULL CHECK (ARCHIVED IN ('0', '1')),
	CONTEXT					VARCHAR2 (99) NOT NULL,
	CREATED_BY_DATE			NUMBER NOT NULL,
	CREATED_BY_USER			VARCHAR2 (99) NOT NULL,
	DATES_ACCEPT_UNTIL		NUMBER,
	DATES_ARCHIVED			NUMBER,
	DATES_DUE				NUMBER,
	DATES_OPEN				NUMBER,
	GRADING_ANONYMOUS		CHAR (1) NOT NULL CHECK (GRADING_ANONYMOUS IN ('0', '1')),
	GRADING_AUTO_RELEASE	CHAR (1) NOT NULL CHECK (GRADING_AUTO_RELEASE IN ('0', '1')),
	GRADING_GRADEBOOK		CHAR (1) NOT NULL CHECK (GRADING_GRADEBOOK IN ('0', '1')),
	GRADING_REJECTED		CHAR (1) NOT NULL CHECK (GRADING_REJECTED IN ('0', '1')),
	HONOR_PLEDGE			CHAR (1) NOT NULL CHECK (HONOR_PLEDGE IN ('0', '1')),
	ID						NUMBER NOT NULL PRIMARY KEY,
	LIVE					CHAR (1) NOT NULL CHECK (LIVE IN ('0', '1')),
	LOCKED					CHAR (1) NOT NULL CHECK (LOCKED IN ('0', '1')),
	MINT					CHAR (1) NOT NULL CHECK (MINT IN ('0', '1')),
	MODIFIED_BY_DATE		NUMBER NOT NULL,
	MODIFIED_BY_USER		VARCHAR2 (99) NOT NULL,
	PARTS_CONTINUOUS		CHAR (1) NOT NULL CHECK (PARTS_CONTINUOUS IN ('0', '1')),
	PARTS_SHOW_PRES			CHAR (1) CHECK (PARTS_SHOW_PRES IN ('0', '1')),
	PASSWORD				VARCHAR2 (255) NULL,
	PRESENTATION_TEXT		CLOB,
	PUBLISHED				CHAR (1) NOT NULL CHECK (PUBLISHED IN ('0', '1')),
	QUESTION_GROUPING		VARCHAR2 (32) NOT NULL,
	RANDOM_ACCESS			CHAR (1) NOT NULL CHECK (RANDOM_ACCESS IN ('0', '1')),
	REVIEW_DATE				NUMBER,
	REVIEW_SHOW_CORRECT		CHAR (1) NOT NULL CHECK (REVIEW_SHOW_CORRECT IN ('0', '1')),
	REVIEW_SHOW_FEEDBACK	CHAR (1) NOT NULL CHECK (REVIEW_SHOW_FEEDBACK IN ('0', '1')),
	REVIEW_TIMING			VARCHAR2 (32) NOT NULL,
	SHOW_HINTS				CHAR (1) NOT NULL CHECK (SHOW_HINTS IN ('0', '1')),
	SUBMIT_PRES_TEXT		CLOB,
	TIME_LIMIT				NUMBER,
	TITLE					VARCHAR2 (255),
	TRIES					NUMBER,
	TYPE					VARCHAR2 (32) NOT NULL
);

CREATE SEQUENCE MNEME_ASSESSMENT_SEQ;

CREATE INDEX MNEME_ASSESSMENT_IDX_CAMHP ON MNEME_ASSESSMENT
(
	CONTEXT		ASC,
	ARCHIVED	ASC,
	MINT		ASC,
	PUBLISHED	ASC
);

CREATE INDEX MNEME_ASSESSMENT_IDX_MINT ON MNEME_ASSESSMENT
(
	MINT		ASC
);

-----------------------------------------------------------------------------

CREATE TABLE MNEME_ASSESSMENT_ACCESS
(
	ASSESSMENT_ID			NUMBER NOT NULL,
	DATES_ACCEPT_UNTIL		NUMBER,
	DATES_DUE				NUMBER,
	DATES_OPEN				NUMBER,
	ID						NUMBER NOT NULL PRIMARY KEY,
	OVERRIDE_ACCEPT_UNTIL	CHAR (1) NOT NULL CHECK (OVERRIDE_ACCEPT_UNTIL IN ('0', '1')),
	OVERRIDE_DUE			CHAR (1) NOT NULL CHECK (OVERRIDE_DUE IN ('0', '1')),
	OVERRIDE_OPEN			CHAR (1) NOT NULL CHECK (OVERRIDE_OPEN IN ('0', '1')),
	OVERRIDE_PASSWORD		CHAR (1) NOT NULL CHECK (OVERRIDE_PASSWORD IN ('0', '1')),
	OVERRIDE_TIME_LIMIT		CHAR (1) NOT NULL CHECK (OVERRIDE_TIME_LIMIT IN ('0', '1')),
	OVERRIDE_TRIES			CHAR (1) NOT NULL CHECK (OVERRIDE_TRIES IN ('0', '1')),
	PASSWORD				VARCHAR2 (255) NULL,
	TIME_LIMIT				NUMBER,
	TRIES					NUMBER,
	USERS					CLOB
);

CREATE SEQUENCE MNEME_ASSESSMENT_ACCESS_SEQ;

CREATE INDEX MNEME_AA_IDX_AID ON MNEME_ASSESSMENT_ACCESS
(
	ASSESSMENT_ID	ASC
);

-----------------------------------------------------------------------------

CREATE TABLE MNEME_ASSESSMENT_PART
(
	ASSESSMENT_ID			NUMBER NOT NULL,
	ID						NUMBER NOT NULL PRIMARY KEY,
	PRESENTATION_TEXT		CLOB,
	SEQUENCE				NUMBER,
	TITLE					VARCHAR2 (255),
	TYPE					CHAR (1) NOT NULL CHECK (TYPE IN ('D', 'M')),
	RANDOMIZE				CHAR (1) NOT NULL CHECK (RANDOMIZE IN ('0', '1'))
);

CREATE SEQUENCE MNEME_ASSESSMENT_PART_SEQ;

CREATE INDEX MNEME_AP_IDX_AID ON MNEME_ASSESSMENT_PART
(
	ASSESSMENT_ID	ASC
);

-----------------------------------------------------------------------------

CREATE TABLE MNEME_ASSESSMENT_PART_DETAIL
(
	ASSESSMENT_ID			NUMBER NOT NULL,
	NUM_QUESTIONS_SEQ		NUMBER,
	ORIG_PID				NUMBER,
	ORIG_QID				NUMBER,
	PART_ID					NUMBER NOT NULL,
	POOL_ID					NUMBER,
	QUESTION_ID				NUMBER
);

CREATE INDEX MNEME_APD_IDX_AID ON MNEME_ASSESSMENT_PART_DETAIL
(
	ASSESSMENT_ID	ASC
);

CREATE INDEX MNEME_APD_IDX_PID ON MNEME_ASSESSMENT_PART_DETAIL
(
	PART_ID	ASC
);
