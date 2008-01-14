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

CREATE TABLE MNEME_POOL
(
	CONTEXT				VARCHAR2 (99) NOT NULL,
	CREATED_BY_DATE		NUMBER NOT NULL,
	CREATED_BY_USER		VARCHAR2 (99) NOT NULL,
	DESCRIPTION			VARCHAR2 (255),
	DIFFICULTY			CHAR (1) NOT NULL CHECK (DIFFICULTY IN (1, 2, 3, 4, 5)),
	HISTORICAL			CHAR (1) NOT NULL CHECK (HISTORICAL IN (0, 1)),
	ID					NUMBER UNSIGNED NOT NULL PRIMARY KEY,
	MINT				CHAR (1) NOT NULL CHECK (MINT IN (0, 1)),
	MODIFIED_BY_DATE	NUMBER NOT NULL,
	MODIFIED_BY_USER	VARCHAR2 (99) NOT NULL,
	POINTS				FLOAT,
	TITLE				VARCHAR2 (255)
);

CREATE SEQUENCE MNEME_POOL_SEQ;

CREATE INDEX MNEME_POOL_IDX_CMH ON MNEME_POOL
(
	CONTEXT		ASC,
	MINT		ASC,
	HISTORICAL	ASC
);

CREATE INDEX MNEME_POOL_IDX_M ON MNEME_POOL
(
	MINT		ASC
);

-----------------------------------------------------------------------------

CREATE TABLE MNEME_POOL_MANIFEST
(
	POOL_ID			NUMBER UNSIGNED NOT NULL,
	QUESTION_ID		NUMBER UNSIGNED NOT NULL
);

ALTER TABLE MNEME_POOL_MANIFEST ADD
(
	PRIMARY KEY (POOL_ID, QUESTION_ID)
);
