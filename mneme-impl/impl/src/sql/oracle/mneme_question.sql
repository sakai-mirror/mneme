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
-- Mneme Question DDL
-----------------------------------------------------------------------------

CREATE TABLE MNEME_QUESTION
(
	CONTEXT				VARCHAR2 (99) NOT NULL,
	CREATED_BY_DATE		NUMBER NOT NULL,
	CREATED_BY_USER		VARCHAR2 (99) NOT NULL,
	DESCRIPTION			VARCHAR2 (255) NULL,
	EXPLAIN_REASON		CHAR (1) NOT NULL CHECK (EXPLAIN_REASON IN ('0', '1')),
	FEEDBACK			CLOB,
	HINTS				CLOB,
	HISTORICAL			CHAR (1) NOT NULL CHECK (HISTORICAL IN ('0', '1')),
	ID					NUMBER NOT NULL PRIMARY KEY,
	MINT				CHAR (1) NOT NULL CHECK (MINT IN ('0', '1')),
	MODIFIED_BY_DATE	NUMBER NOT NULL,
	MODIFIED_BY_USER	VARCHAR2 (99) NOT NULL,
	POOL_ID				NUMBER NULL,
	PRESENTATION_TEXT	CLOB,
	TYPE				VARCHAR2 (99) NOT NULL,
	GUEST				CLOB
);

CREATE SEQUENCE MNEME_QUESTION_SEQ;

CREATE INDEX MNEME_QUESTION_IDX_MHP ON MNEME_QUESTION
(
	MINT		ASC,
	HISTORICAL	ASC,
	POOL_ID		ASC
);
