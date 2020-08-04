/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.util

import org.gradle.internal.Cast
import spock.lang.Shared
import spock.lang.Specification

import static com.google.common.collect.Iterables.concat
import static com.google.common.collect.Lists.newArrayList
import static java.util.Collections.singletonMap
import static org.gradle.util.WrapUtil.toList

/*TODO rename to NameMatcherSpec and remove origin */
class NameMatcherSpec extends Specification {

    @Shared
    NameMatcher matcher

    def setup() {
        matcher = new NameMatcher()
    }

    def "selects exact match"() {
        expect:
        matches("name", "name")
        matches("name", "name", "other")
    }

    /*TODO convert to parameterized test */
    def "selects item with matching prefix"() {
        expect:
        matches("na", "name")
        matches("na", "name", "other")
        // Mixed case
        matches("na", "Name")
        matches("NA", "name")
        matches("somena", "someName")
        matches("somena", "SomeName")
        matches("somena", "SomeName")
        matches("some na", "Some Name")
    }

    def "selects item with matching camel case prefix"() {
        expect:
        matches("sN", "someName")
        matches("soN", "someName")
        matches("SN", "someName")
        matches("SN", "SomeName")
        matches("SN", "SomeNameWithExtraStuff")
        matches("so_n", "some_name")
        matches("so_n", "some_Name")
        matches("so_n_wi_ext", "some_Name_with_EXTRA")
        matches("so.n", "some.name")
        matches("so n", "some name")
        matches("ABC", "ABC")
        matches("a9N", "a9Name")
        matches("a9N", "abc9Name")
        matches("a9n", "abc9Name")
    }

    def "prefers exact match over case insensitive match"() {
        expect:
        matches("name", "name", "Name", "NAME")
        matches("someName", "someName", "SomeName", "somename", "SOMENAME")
        matches("some Name", "some Name", "Some Name", "some name", "SOME NAME")
    }

    def "prefers exact match over partial match"() {
        expect:
        matches("name", "name", "nam", "n", "NAM")
    }

    def "prefers exact match over prefix match"() {
        expect:
        matches("someName", "someName", "someNameWithExtra")
    }

    def "prefers exact match over camel case match"() {
        expect:
        matches("sName", "sName", "someName", "sNames")
        matches("so Name", "so Name", "some Name", "so name")
        matches("ABC", "ABC", "AaBbCc")
    }

    def "prefers full camel case match over camel case prefix"() {
        expect:
        matches("sN", "someName", "someNameWithExtra")
        matches("name", "names", "nameWithExtra")
        matches("s_n", "some_name", "some_name_with_extra")
    }

    def "prefers case sensitive camel case match over case insensitive camel case match"() {
        expect:
        matches("soNa", "someName", "somename")
        matches("SN", "SomeName", "someName")
        matches("na1", "name1", "Name1", "NAME1")
    }

    def "prefers case insensitive match over camel case match"() {
        expect:
        matches("somename", "someName", "someNameWithExtra")
        matches("soNa", "sona", "someName")
    }

    def "does not select items when no matches"() {
        expect:
        doesNotMatch("name")
        doesNotMatch("name", "other")
        doesNotMatch("name", "na")
        doesNotMatch("sN", "otherName")
        doesNotMatch("sA", "someThing")
        doesNotMatch("soN", "saN")
        doesNotMatch("soN", "saName")
    }

    def "does not select items when multiple camel case matches"() {
        expect:
        matcher.find("sN", toList("someName", "soNa", "other")) == null
    }

    def "does not select items when multiple case insensitive matches"() {
        expect:
        matcher.find("someName", toList("somename", "SomeName", "other")) == null
        matcher.getMatches() == ["somename", "SomeName"] as Set
    }

    def "empty pattern does not select anything"() {
        expect:
        doesNotMatch("", "something")
    }

    def "escapes regexp chars"() {
        expect:
        doesNotMatch("name\\othername", "other")
    }

    def "reports potential matches"() {
        expect:
        matcher.find("name", toList("tame", "lame", "other")) == null
        matcher.getMatches().empty // TODO convert all getters to accessors
        matcher.getCandidates() == ["tame", "lame"] as Set
    }

    def "does not select map entry when no matches"() {
        expect:
        matcher.find("soNa", singletonMap("does not match", 9)) == null
    }

    def "selects map entry when exact match"() {
        expect:
        matcher.find("name", singletonMap("name", 9)) == 9
    }

    def "selects map entry when one partial match"() {
        expect:
        matcher.find("soNa", singletonMap("someName", 9)) == 9
    }

    def "does not select map entry when multiple partial matches"() {
        expect:
        Map<String, Integer> items = Cast.uncheckedNonnullCast(GUtil.map("someName", 9, "soName", 10))
        matcher.find("soNa", items) == null
    }

    def "builds error message for no matches"() {
        setup:
        matcher.find("name", toList("other"))

        expect: // TODO remove assertThat everywhere
        matcher.formatErrorMessage("thing", "container") == "Thing 'name' not found in container."
    }

    def "builds error message for multiple matches"() {
        setup:
        matcher.find("n", toList("number", "name", "other"))

        expect:
        matcher.formatErrorMessage("thing", "container") == "Thing 'n' is ambiguous in container. Candidates are: 'name', 'number'."
    }

    def "builds error message for potential matches"() {
        setup:
        matcher.find("name", toList("other", "lame", "tame"))

        expect:
        matcher.formatErrorMessage("thing", "container") == "Thing 'name' not found in container. Some candidates are: 'lame', 'tame'."
    }

    /*TODO rename to matches() */
    def matches(String name, String match, String... extraItems) {
        List<String> allItems = newArrayList(concat(toList(match), toList(extraItems))) // TODO make groovier
        matcher.find(name, allItems) == match && matcher.getMatches() == [match] as Set
    }

    def doesNotMatch(String name, String... items) {
        matcher.find(name, toList(items)) == null && matcher.matches.empty
    }
}
