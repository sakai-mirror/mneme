--*********************************************************************************
-- $URL$
-- $Id$
--**********************************************************************************
--
-- Copyright (c) 2008 Etudes, Inc.
-- 
-- Portions completed before September 1, 2008
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
	ARCHIVED				CHAR (1),
	CONTEXT					VARCHAR2 (99),
	CREATED_BY_DATE			NUMBER,
	CREATED_BY_USER			VARCHAR2 (99),
	DATES_ACCEPT_UNTIL		NUMBER,
	DATES_ARCHIVED			NUMBER,
	DATES_DUE				NUMBER,
	DATES_OPEN				NUMBER,
	GRADING_ANONYMOUS		CHAR (1),
	GRADING_AUTO_RELEASE	CHAR (1),
	GRADING_GRADEBOOK		CHAR (1),
	GRADING_REJECTED		CHAR (1),
	HONOR_PLEDGE			CHAR (1),
	ID						NUMBER NOT NULL PRIMARY KEY,
	LIVE					CHAR,
	LOCKED					CHAR,
	MINT					CHAR,
	MODIFIED_BY_DATE		NUMBER,
	MODIFIED_BY_USER		VARCHAR2 (99),
	PARTS_CONTINUOUS		CHAR (1),
	PARTS_SHOW_PRES			CHAR (1),
	PASSWORD				VARCHAR2 (255 CHAR),
	PRESENTATION_TEXT		CLOB,
	PUBLISHED				CHAR (1),
	QUESTION_GROUPING		VARCHAR2 (32),
	RANDOM_ACCESS			CHAR (1),
	REVIEW_DATE				NUMBER,
	REVIEW_SHOW_CORRECT		CHAR (1),
	REVIEW_SHOW_FEEDBACK	CHAR (1),
	REVIEW_TIMING			VARCHAR2 (32),
	SHOW_HINTS				CHAR (1),
	SUBMIT_PRES_TEXT		CLOB,
	TIME_LIMIT				NUMBER,
	TITLE					VARCHAR2 (255 CHAR),
	TRIES					NUMBER,
	TYPE					VARCHAR2 (32)
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
	ASSESSMENT_ID			NUMBER,
	DATES_ACCEPT_UNTIL		NUMBER,
	DATES_DUE				NUMBER,
	DATES_OPEN				NUMBER,
	ID						NUMBER NOT NULL PRIMARY KEY,
	OVERRIDE_ACCEPT_UNTIL	CHAR (1),
	OVERRIDE_DUE			CHAR (1),
	OVERRIDE_OPEN			CHAR (1),
	OVERRIDE_PASSWORD		CHAR (1),
	OVERRIDE_TIME_LIMIT		CHAR (1),
	OVERRIDE_TRIES			CHAR (1),
	PASSWORD				VARCHAR2 (255 CHAR),
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
	ASSESSMENT_ID			NUMBER,
	ID						NUMBER NOT NULL PRIMARY KEY,
	PRESENTATION_TEXT		CLOB,
	SEQUENCE				NUMBER,
	TITLE					VARCHAR2 (255 CHAR),
	TYPE					CHAR (1),
	RANDOMIZE				CHAR (1)
);

CREATE SEQUENCE MNEME_ASSESSMENT_PART_SEQ;

CREATE INDEX MNEME_AP_IDX_AID ON MNEME_ASSESSMENT_PART
(
	ASSESSMENT_ID	ASC
);

-----------------------------------------------------------------------------

CREATE TABLE MNEME_ASSESSMENT_PART_DETAIL
(
	ASSESSMENT_ID			NUMBER,
	NUM_QUESTIONS_SEQ		NUMBER,
	ORIG_PID				NUMBER,
	ORIG_QID				NUMBER,
	PART_ID					NUMBER,
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
