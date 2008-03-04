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
-- Mneme Submission DDL changes between 1.0 and 1.1
-----------------------------------------------------------------------------

ALTER TABLE MNEME_SUBMISSION CHANGE USER USERID VARCHAR (99) NOT NULL;

DROP INDEX MNEME_SUBMISSION_IDX_AUC ON MNEME_SUBMISSION;

CREATE INDEX MNEME_SUBMISSION_IDX_AUC ON MNEME_SUBMISSION
(
	ASSESSMENT_ID		ASC,
	USERID				ASC,
	COMPLETE			ASC
);

ALTER TABLE MNEME_SUBMISSION
	CHANGE EVAL_COMMENT EVAL_COMMENT LONGTEXT,
	CHANGE ASSESSMENT_ID ASSESSMENT_ID BIGINT UNSIGNED;

ALTER TABLE MNEME_ANSWER
	CHANGE GUEST GUEST LONGTEXT,
	CHANGE EVAL_COMMENT EVAL_COMMENT LONGTEXT,
	CHANGE REASON REASON LONGTEXT,
	CHANGE SUBMISSION_ID SUBMISSION_ID BIGINT UNSIGNED,
	CHANGE PART_ID PART_ID BIGINT UNSIGNED,
	CHANGE QUESTION_ID QUESTION_ID BIGINT UNSIGNED;

ALTER TABLE MNEME_SUBMISSION ADD (EVAL_ATTACHMENTS LONGTEXT);
ALTER TABLE MNEME_ANSWER ADD (EVAL_ATTACHMENTS LONGTEXT);
