--*********************************************************************************
-- $URL$
-- $Id$
--**********************************************************************************
--
-- Copyright (c) 2007 The Regents of the University of Michigan & Foothill College, ETUDES Project
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
-- Mneme Pool DDL
-----------------------------------------------------------------------------

CREATE TABLE MNEME_SUBMISSION
(
	ASSESSMENT_ID		BIGINT NOT NULL,
	COMPLETE			CHAR (1) NOT NULL CHECK (COMPLETE IN (0, 1)),
	CONTEXT				VARCHAR (99) NOT NULL,
	EVAL_ATRIB_DATE		BIGINT,
	EVAL_ATRIB_USER		VARCHAR (99),
	EVAL_COMMENT		TEXT,
	EVAL_EVALUATED		CHAR (1) NOT NULL CHECK (EVAL_EVALUATED IN (0, 1)),
	EVAL_SCORE			FLOAT,
	ID					BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
	RELEASED			CHAR (1) NOT NULL CHECK (RELEASED IN (0, 1)),
	START_DATE			BIGINT,
	SUBMITTED_DATE		BIGINT,
	USER				VARCHAR (99) NOT NULL
);

-----------------------------------------------------------------------------

CREATE TABLE MNEME_ANSWER
(
	GUEST				MEDIUMTEXT,
	EVAL_ATRIB_DATE		BIGINT,
	EVAL_ATRIB_USER		VARCHAR (99),
	EVAL_COMMENT		TEXT,
	EVAL_EVALUATED		CHAR (1) NOT NULL CHECK (EVAL_EVALUATED IN (0, 1)),
	EVAL_SCORE			FLOAT,
	ID					BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
	ORIG_PID			BIGINT NOT NULL,
	PART_ID				BIGINT NOT NULL,
	QUESTION_ID			BIGINT NOT NULL,
	QUESTION_TYPE		VARCHAR (99) NOT NULL,
	REASON				TEXT,
	REVIEW				CHAR (1) NOT NULL CHECK (REVIEW IN (0, 1)),
	SUBMISSION_ID		BIGINT NOT NULL,
	SUBMITTED_DATE		BIGINT
);
