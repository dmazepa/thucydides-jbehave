package net.thucydides.jbehave;

import net.thucydides.core.guice.Injectors;
import net.thucydides.core.model.TestOutcome;
import net.thucydides.core.model.TestResult;
import net.thucydides.core.model.TestStep;
import net.thucydides.core.model.TestTag;
import net.thucydides.core.util.EnvironmentVariables;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CancellationException;

import static net.thucydides.core.matchers.PublicThucydidesMatchers.containsResults;
import static net.thucydides.core.model.TestResult.FAILURE;
import static net.thucydides.core.model.TestResult.PENDING;
import static net.thucydides.core.model.TestResult.SKIPPED;
import static net.thucydides.core.model.TestResult.SUCCESS;
import static net.thucydides.core.reports.matchers.TestOutcomeMatchers.havingTag;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

public class WhenRunningJBehaveStories extends AbstractJBehaveStory {

    final static class AllStoriesSample extends ThucydidesJUnitStories {}

    final static class AStorySample extends ThucydidesJUnitStories {
        AStorySample(String storyName) {
            findStoriesCalled(storyName);
        }
    }

    @Test
    public void all_stories_on_the_classpath_should_be_run_by_default() throws Throwable {

        // Given
        ThucydidesJUnitStories stories = new AllStoriesSample();
        assertThat(stories.getRootPackage(), is("net.thucydides.jbehave"));
        assertThat(stories.getStoryPath(), is("**/*.story"));
    }

    final static class StoriesInTheSubsetFolderSample extends ThucydidesJUnitStories {
        StoriesInTheSubsetFolderSample() {
            findStoriesIn("stories/subset");
        }
    }


    @Test
    public void a_story_should_read_properties_from_the_thucydides_properties_file() throws Throwable {

        // Given
        ThucydidesJUnitStories stories = new StoriesInTheSubsetFolderSample();

        // When
        run(stories);

        // Then
        EnvironmentVariables environmentVariables = Injectors.getInjector().getInstance(EnvironmentVariables.class);
        assertThat(environmentVariables.getProperty("story.timeout.in.secs"), is("350"));
    }


    @Test
    public void a_subset_of_the_stories_can_be_run_individually() throws Throwable {

        // Given
        ThucydidesJUnitStories stories = new StoriesInTheSubsetFolderSample();
        stories.setSystemConfiguration(systemConfiguration);

        // When
        run(stories);

        // Then

        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.size(), is(5));
    }

    @Test
    public void stories_with_a_matching_name_can_be_run() throws Throwable {

        // Given
        ThucydidesJUnitStories stories = new AStorySample("*PassingBehavior.story");
        stories.setSystemConfiguration(systemConfiguration);

        // When
        run(stories);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.size(), is(6));
    }

    @Test
    public void pending_stories_should_be_reported_as_pending() throws Throwable {

        // Given
        ThucydidesJUnitStories pendingStory = new AStorySample("aPendingBehavior.story");

        pendingStory.setSystemConfiguration(systemConfiguration);

        // When
        run(pendingStory);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.size(), is(1));
        assertThat(outcomes.get(0).getResult(), is(TestResult.PENDING));
    }

    @Test
    public void pending_stories_should_report_the_given_when_then_steps() throws Throwable {

        // Given
        ThucydidesJUnitStories pendingStory = new AStorySample("aPendingBehavior.story");

        pendingStory.setSystemConfiguration(systemConfiguration);

        // When
        run(pendingStory);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.size(), is(1));
        assertThat(outcomes.get(0).getStepCount(), is(4));
    }

    @Test
    public void implemented_pending_stories_should_be_reported_as_pending() throws Throwable {

        // Given
        ThucydidesJUnitStories pendingStory = new AStorySample("aPendingImplementedBehavior.story");

        pendingStory.setSystemConfiguration(systemConfiguration);

        // When
        run(pendingStory);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.size(), is(1));
        assertThat(outcomes.get(0).getResult(), is(TestResult.PENDING));
    }

    @Test
    public void passing_stories_should_be_reported_as_passing() throws Throwable {

        // Given
        ThucydidesJUnitStories passingStory = new AStorySample("aPassingBehavior.story");

        passingStory.setSystemConfiguration(systemConfiguration);

        // When
        run(passingStory);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.size(), is(1));
        assertThat(outcomes.get(0).getResult(), is(TestResult.SUCCESS));
    }

    @Test
    public void state_should_be_conserved_between_steps() throws Throwable {

        // Given
        ThucydidesJUnitStories passingStoryWithState = new AStorySample("aPassingBehaviorWithState.story");

        passingStoryWithState.setSystemConfiguration(systemConfiguration);

        // When
        run(passingStoryWithState);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.size(), is(1));
        assertThat(outcomes.get(0).getResult(), is(TestResult.SUCCESS));
    }

    @Test
    public void state_should_not_be_conserved_between_stories() throws Throwable {

        // Given
        ThucydidesJUnitStories passingStoryWithState = new AStorySample("*PassingBehaviorWithState.story");

        passingStoryWithState.setSystemConfiguration(systemConfiguration);

        // When
        run(passingStoryWithState);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.size(), is(2));
        assertThat(outcomes.get(0).getResult(), is(TestResult.SUCCESS));
        assertThat(outcomes.get(1).getResult(), is(TestResult.SUCCESS));
    }

    @Test
    public void a_passing_story_with_steps_should_record_the_steps() throws Throwable {

        // Given
        ThucydidesJUnitStories passingStory = new AStorySample("aPassingBehaviorWithSteps.story");

        passingStory.setSystemConfiguration(systemConfiguration);

        // When
        run(passingStory);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.size(), is(1));
        assertThat(outcomes.get(0).getResult(), is(TestResult.SUCCESS));
        assertThat(outcomes.get(0).getNestedStepCount(), is(7));
    }

    @Test
    public void the_given_when_then_clauses_should_count_as_steps() throws Throwable {

        // Given
        ThucydidesJUnitStories passingStory = new AStorySample("aPassingBehaviorWithSteps.story");

        passingStory.setSystemConfiguration(systemConfiguration);

        // When
        run(passingStory);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();

        List<TestStep> steps = outcomes.get(0).getTestSteps();
        assertThat(steps.get(0).getDescription(), is("Given I have an implemented JBehave scenario"));
        assertThat(steps.get(1).getDescription(), is("And the scenario has steps"));
        assertThat(steps.get(2).getDescription(), is("When I run the scenario"));
        assertThat(steps.get(3).getDescription(), is("Then the steps should appear in the outcome"));
    }

    @Test
    public void failing_stories_should_be_reported_as_failing() throws Throwable {

        // Given
        ThucydidesJUnitStories failingStory = new AStorySample("aFailingBehavior.story");

        failingStory.setSystemConfiguration(systemConfiguration);

        // When
        run(failingStory);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.size(), is(1));
        assertThat(outcomes.get(0).getResult(), is(TestResult.FAILURE));
    }

    @Test
    public void steps_after_a_failing_step_should_be_skipped() throws Throwable {

        // Given
        ThucydidesJUnitStories story = new AStorySample("aComplexFailingBehavior.story");

        story.setSystemConfiguration(systemConfiguration);

        // When
        run(story);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.size(), is(1));

        // And
        assertThat(outcomes.get(0), containsResults(SUCCESS, FAILURE, SKIPPED, SKIPPED, SKIPPED));
    }

    @Test
    public void a_test_with_a_pending_step_should_be_pending() throws Throwable {

        // Given
        ThucydidesJUnitStories story = new AStorySample("aBehaviorWithAPendingStep.story");

        story.setSystemConfiguration(systemConfiguration);

        // When
        run(story);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.size(), is(1));

        // And
        assertThat(outcomes.get(0).getResult() , is(PENDING));
        // And
        assertThat(outcomes.get(0), containsResults(SUCCESS, SUCCESS, SUCCESS, SUCCESS, SUCCESS, SUCCESS, PENDING, PENDING, SUCCESS));

    }

    @Test
    public void a_test_should_be_associated_with_a_corresponding_issue_if_specified() throws Throwable {

        // Given
        ThucydidesJUnitStories story = new AStorySample("aBehaviorWithAnIssue.story");

        story.setSystemConfiguration(systemConfiguration);

        // When
        run(story);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.size(), is(1));
        assertThat(outcomes.get(0).getIssueKeys(), hasItem("MYPROJ-456"));

    }

    @Test
    public void a_test_can_be_associated_with_several_issues() throws Throwable {

        // Given
        ThucydidesJUnitStories story = new AStorySample("aBehaviorWithMultipleIssues.story");

        story.setSystemConfiguration(systemConfiguration);

        // When
        run(story);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.get(0).getIssueKeys(), hasItems("MYPROJ-6", "MYPROJ-7"));

    }

    @Test(expected = AssertionError.class)
    public void a_test_running_a_failing_story_should_fail() throws Throwable {
        ThucydidesJUnitStories stories = new AFailingBehavior();
        stories.setSystemConfiguration(systemConfiguration);
        stories.run();
    }

    @Test
    public void a_test_running_a_failing_story_should_not_fail_if_ignore_failures_in_stories_is_set_to_true() throws Throwable {

        environmentVariables.setProperty("ignore.failures.in.stories","true");

        ThucydidesJUnitStories stories = new AFailingBehavior(environmentVariables);
        stories.setSystemConfiguration(systemConfiguration);
        stories.run();
    }

    @Test(expected = AssertionError.class)
    public void a_test_running_a_failing_story_among_several_should_fail() throws Throwable {
        ThucydidesJUnitStories stories = new ASetOfBehaviorsContainingFailures();
        stories.setSystemConfiguration(systemConfiguration);
        stories.run();
    }

    @Test(expected = AssertionError.class)
    public void a_test_running_a_slow_story_should_fail_if_it_timesout() throws Throwable {
        environmentVariables.setProperty("story.timeout.in.secs","1");
        ThucydidesJUnitStories stories = new ABehaviorContainingSlowTests(environmentVariables);

        stories.run();
    }

    @Test
    public void a_test_running_a_slow_story_should_not_fail_if_it_does_not_timeout() throws Throwable {
        environmentVariables.setProperty("story.timeout.in.secs","100");
        ThucydidesJUnitStories stories = new ABehaviorContainingSlowTests(environmentVariables);

        stories.run();
    }

    @Ignore("Will run JBehave stories individually one day, maybe")
    @Test
    public void timeouts_refer_only_in_individual_stories() throws Throwable {
        environmentVariables.setProperty("story.timeout.in.secs","3");
        ThucydidesJUnitStories stories = new ASetOfBehaviorsContainingSlowTests(environmentVariables);

        stories.run();
    }

    @Test
    public void a_test_story_can_be_associated_with_several_issues() throws Throwable {

        // Given
        ThucydidesJUnitStories story = new AStorySample("aBehaviorWithOneStoryAndMultipleIssues.story");

        story.setSystemConfiguration(systemConfiguration);

        // When
        run(story);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.get(0).getIssueKeys(), hasItems("MYPROJ-6","MYPROJ-7","MYPROJ-8"));

    }
    @Test
    public void all_the_scenarios_in_a_story_should_be_associated_with_a_corresponding_issue_if_specified_at_the_story_level() throws Throwable {

        // Given
        ThucydidesJUnitStories story = new AStorySample("aBehaviorWithIssues.story");

        story.setSystemConfiguration(systemConfiguration);

        // When
        run(story);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.size(), is(2));
        assertThat(outcomes.get(0).getIssueKeys(), hasItems("MYPROJ-123", "MYPROJ-456"));
        assertThat(outcomes.get(1).getIssueKeys(), hasItems("MYPROJ-123", "MYPROJ-789"));
    }


    @Test
    public void a_test_should_have_a_story_tag_matching_the_jbehave_story() throws Throwable {

        // Given
        ThucydidesJUnitStories story = new AStorySample("aBehaviorWithAnIssue.story");

        story.setSystemConfiguration(systemConfiguration);

        // When
        run(story);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.get(0), havingTag(TestTag.withName("A behavior with an issue").andType("story")));
    }

    @Test
    public void a_test_should_have_features_defined_by_the_feature_meta_field() throws Throwable {

        // Given
        ThucydidesJUnitStories story = new AStorySample("aBehaviorWithFeatures.story");

        story.setSystemConfiguration(systemConfiguration);

        // When
        run(story);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.get(0), havingTag(TestTag.withName("a feature").andType("feature")));
    }

    @Test
    public void a_test_should_have_features_defined_at_the_story_levelby_the_feature_meta_field() throws Throwable {

        // Given
        ThucydidesJUnitStories story = new AStorySample("aBehaviorWithFeatures.story");

        story.setSystemConfiguration(systemConfiguration);

        // When
        run(story);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.get(0), havingTag(TestTag.withName("a feature").andType("feature")));
        assertThat(outcomes.get(0), havingTag(TestTag.withName("another feature").andType("feature")));
    }


    @Test
    public void a_test_should_have_multiple_features_defined_at_the_story_level_by_the_feature_meta_field() throws Throwable {

        // Given
        ThucydidesJUnitStories story = new AStorySample("aBehaviorWithMultipleFeatures.story");

        story.setSystemConfiguration(systemConfiguration);

        // When
        run(story);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.get(0), havingTag(TestTag.withName("a feature").andType("feature")));
        assertThat(outcomes.get(0), havingTag(TestTag.withName("another feature").andType("feature")));
        assertThat(outcomes.get(0), havingTag(TestTag.withName("yet another feature").andType("feature")));
    }


    @Test
    public void a_test_should_have_tags_defined_by_the_tag_meta_field() throws Throwable {

        // Given
        ThucydidesJUnitStories story = new AStorySample("aBehaviorWithTags.story");

        story.setSystemConfiguration(systemConfiguration);

        // When
        run(story);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.get(0), havingTag(TestTag.withName("a domain").andType("domain")));
    }

    @Test
    public void a_test_should_have_storywide_tags_defined_by_the_tag_meta_field() throws Throwable {

        // Given
        ThucydidesJUnitStories story = new AStorySample("aBehaviorWithTags.story");

        story.setSystemConfiguration(systemConfiguration);

        // When
        run(story);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.get(0), havingTag(TestTag.withName("a domain").andType("domain")));
        assertThat(outcomes.get(0), havingTag(TestTag.withName("a capability").andType("capability")));
        assertThat(outcomes.get(0), havingTag(TestTag.withName("iteration 1").andType("iteration")));
    }

    @Test(expected = AssertionError.class)
    public void failing_stories_run_in_junit_should_fail() throws Throwable {

        // Given
        ThucydidesJUnitStories failingStory = new AStorySample("aFailingBehavior.story");

        failingStory.setSystemConfiguration(systemConfiguration);

        // When
        failingStory.run();
    }

    @Test(expected = AssertionError.class)
    public void stories_with_errors_run_in_junit_should_fail() throws Throwable {

        // Given
        ThucydidesJUnitStories failingStory = new AStorySample("aBehaviorThrowingAnException.story");

        failingStory.setSystemConfiguration(systemConfiguration);

        // When
        failingStory.run();
    }

    @Test
    public void stories_with_errors_should_be_reported_as_failing() throws Throwable {

        // Given
        ThucydidesJUnitStories failingStory = new AStorySample("aBehaviorThrowingAnException.story");

        failingStory.setSystemConfiguration(systemConfiguration);

        // When
        run(failingStory);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.size(), is(1));
        assertThat(outcomes.get(0).getResult(), is(TestResult.FAILURE));
    }

    @Test
    public void environment_specific_stories_should_be_executed_if_the_corresponding_environment_variable_is_set() throws Throwable {

        // Given
        environmentVariables.setProperty("metafilter","+environment uat");
        ThucydidesJUnitStories uatStory = new ABehaviorForUatOnly(environmentVariables);

        uatStory.setSystemConfiguration(systemConfiguration);

        // When
        run(uatStory);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.size(), is(1));
        assertThat(outcomes.get(0).getResult(), is(TestResult.SUCCESS));
    }

    @Test
    public void environment_specific_stories_should_not_be_executed_if_a_filter_excludes_it() throws Throwable {

        environmentVariables.setProperty("metafilter","-environment uat");
        // Given
        ThucydidesJUnitStories uatStory = new ABehaviorForUatOnly(environmentVariables);

        uatStory.setSystemConfiguration(systemConfiguration);

        // When
        run(uatStory);

        // Then
        List<TestOutcome> outcomes = loadTestOutcomes();
        assertThat(outcomes.size(), is(0));
    }}
