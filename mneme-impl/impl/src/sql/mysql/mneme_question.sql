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
-- Mneme Question DDL
-----------------------------------------------------------------------------

CREATE TABLE MNEME_QUESTION
(
	CONTEXT				VARCHAR (99) NOT NULL,
	CREATED_BY_DATE		BIGINT NOT NULL,
	CREATED_BY_USER		VARCHAR (99) NOT NULL,
	DESCRIPTION			VARCHAR (255) NULL,
	EXPLAIN_REASON		CHAR (1) NOT NULL CHECK (HISTORICAL IN (0, 1)),
	FEEDBACK			TEXT,
	HINTS				TEXT,
	HISTORICAL			CHAR (1) NOT NULL CHECK (HISTORICAL IN (0, 1)),
	ID					BIGINT UNSIGNED AUTO_INCREMENT NOT NULL PRIMARY KEY,
	MINT				CHAR (1) NOT NULL CHECK (MINT IN (0, 1)),
	MODIFIED_BY_DATE	BIGINT NOT NULL,
	MODIFIED_BY_USER	VARCHAR (99) NOT NULL,
	POOL_ID				BIGINT UNSIGNED NULL,
	PRESENTATION_TEXT	TEXT,
	TYPE				VARCHAR (99) NOT NULL,
	GUEST				MEDIUMTEXT
);

CREATE INDEX MNEME_QUESTION_IDX_MHP ON MNEME_QUESTION
(
	MINT		ASC,
	HISTORICAL	ASC,
	POOL_ID		ASC
);