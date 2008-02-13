--*********************************************************************************
-- $URL$
-- $Id$
--**********************************************************************************
--
-- Copyright (c) 2008 The Regents of the University of Michigan & Foothill College, ETUDES Project
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
-- Mneme Question DDL changes between 1.0 and 1.1
-----------------------------------------------------------------------------

ALTER TABLE MNEME_QUESTION ADD (SURVEY CHAR (1) DEFAULT '0' NOT NULL CHECK (SURVEY IN ('0', '1')));

DROP INDEX MNEME_QUESTION_IDX_MHPS ON MNEME_QUESTION;

CREATE INDEX MNEME_QUESTION_IDX_MHPS ON MNEME_QUESTION
(
	MINT		ASC,
	HISTORICAL	ASC,
	POOL_ID		ASC,
	SURVEY		ASC
);
