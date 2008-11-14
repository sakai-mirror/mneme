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
-- Mneme Submission DDL
-----------------------------------------------------------------------------

CREATE TABLE MNEME_SUBMISSION
(
	ASSESSMENT_ID		NUMBER,
	COMPLETE			CHAR (1),
	CONTEXT				VARCHAR2 (99),
	EVAL_ATRIB_DATE		NUMBER,
	EVAL_ATRIB_USER		VARCHAR2 (99),
	EVAL_ATTACHMENTS	CLOB,
	EVAL_COMMENT		CLOB,
	EVAL_EVALUATED		CHAR (1),
	EVAL_SCORE			FLOAT,
	ID					NUMBER NOT NULL PRIMARY KEY,
	RELEASED			CHAR (1),
	START_DATE			NUMBER,
	SUBMITTED_DATE		NUMBER,
	TEST_DRIVE			CHAR (1),
	USERID				VARCHAR2 (99)
);

CREATE SEQUENCE MNEME_SUBMISSION_SEQ;

CREATE INDEX MNEME_SUBMISSION_IDX_ACT ON MNEME_SUBMISSION
(
	ASSESSMENT_ID		ASC,
	COMPLETE			ASC,
	TEST_DRIVE			ASC
);

CREATE INDEX MNEME_SUBMISSION_IDX_AUC ON MNEME_SUBMISSION
(
	ASSESSMENT_ID		ASC,
	USERID				ASC,
	COMPLETE			ASC
);

CREATE INDEX MNEME_SUBMISSION_IDX_CTU ON MNEME_SUBMISSION
(
	CONTEXT				ASC,
	TEST_DRIVE			ASC,
	USERID				ASC
);

CREATE INDEX MNEME_SUBMISSION_IDX_COMPLETE ON MNEME_SUBMISSION
(
	COMPLETE			ASC
);

-----------------------------------------------------------------------------

CREATE TABLE MNEME_ANSWER
(
	ANSWERED			CHAR (1),
	AUTO_SCORE			FLOAT,
	GUEST				CLOB,
	EVAL_ATRIB_DATE		NUMBER,
	EVAL_ATRIB_USER		VARCHAR2 (99),
	EVAL_ATTACHMENTS	CLOB,
	EVAL_COMMENT		CLOB,
	EVAL_EVALUATED		CHAR (1),
	EVAL_SCORE			FLOAT,
	ID					NUMBER NOT NULL PRIMARY KEY,
	PART_ID				NUMBER,
	QUESTION_ID			NUMBER,
	QUESTION_TYPE		VARCHAR2 (99),
	REASON				CLOB,
	REVIEW				CHAR (1),
	SUBMISSION_ID		NUMBER,
	SUBMITTED_DATE		NUMBER
);

CREATE SEQUENCE MNEME_ANSWER_SEQ;

CREATE INDEX MNEME_ANSWER_IDX_QID ON MNEME_ANSWER
(
	QUESTION_ID			ASC
);

CREATE INDEX MNEME_ANSWER_IDX_SID ON MNEME_ANSWER
(
	SUBMISSION_ID		ASC
);
