// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.nereids.rules.exploration.join;

import org.apache.doris.common.Pair;
import org.apache.doris.nereids.rules.Rule;
import org.apache.doris.nereids.rules.RuleType;
import org.apache.doris.nereids.rules.exploration.OneExplorationRuleFactory;

/**
 * Rule for change inner join LAsscom (associative and commutive).
 */
public class OuterJoinLAsscomProject extends OneExplorationRuleFactory {
    public static final OuterJoinLAsscomProject INSTANCE = new OuterJoinLAsscomProject();

    /*
     *        topJoin                   newTopJoin
     *        /     \                   /        \
     *    project    C          newLeftProject newRightProject
     *      /            ──►          /            \
     * bottomJoin                newBottomJoin      B
     *    /   \                     /   \
     *   A     B                   A     C
     */
    @Override
    public Rule build() {
        return logicalJoin(logicalProject(logicalJoin()), group())
                .when(topJoin -> OuterJoinLAsscom.check(topJoin, topJoin.left().child()))
                .when(join -> OuterJoinLAsscom.VALID_TYPE_PAIR_SET.contains(
                        Pair.of(join.left().child().getJoinType(), join.getJoinType())))
                .then(topJoin -> {
                    JoinLAsscomHelper helper = new JoinLAsscomHelper(topJoin, topJoin.left().child());
                    helper.initProject(topJoin.left());
                    if (!helper.initJoinOnCondition()) {
                        return null;
                    }
                    return helper.newTopJoin();
                }).toRule(RuleType.LOGICAL_OUTER_JOIN_LASSCOM_PROJECT);
    }
}