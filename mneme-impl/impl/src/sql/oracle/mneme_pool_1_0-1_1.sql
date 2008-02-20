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
-- Mneme Pool DDL changes between 1.0 and 1.1
-----------------------------------------------------------------------------

DROP TABLE MNEME_POOL_MANIFEST;

ALTER TABLE MNEME_POOL ADD D2 CLOB;
UPDATE MNEME_POOL SET D2=DESCRIPTION;
ALTER TABLE MNEME_POOL DROP COLUMN DESCRIPTION;
ALTER TABLE MNEME_POOL RENAME COLUMN D2 TO DESCRIPTION;
