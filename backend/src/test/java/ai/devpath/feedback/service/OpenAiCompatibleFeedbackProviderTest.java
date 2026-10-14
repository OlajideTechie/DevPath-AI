package ai.devpath.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.devpath.curriculum.catalog.CuratedLessonCatalog;
import ai.devpath.feedback.dto.AdvisoryFeedback;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class OpenAiCompatibleFeedbackProviderTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static ValidatorFactory validatorFactory;
    private static OpenAiCompatibleFeedbackProvider provider;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        Validator validator = validatorFactory.getValidator();
        provider = new OpenAiCompatibleFeedbackProvider(
                OBJECT_MAPPER,
                validator,
                "https://example.test/v1",
                "test-key",
                "test-model",
                Duration.ofSeconds(1),
                Duration.ofSeconds(1));
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void correctSolutionWithNegativeBalanceGuardIsAcceptedAsAdditionalDefense() throws Exception {
        String code = """
                public static boolean canWithdraw(BigDecimal balance, BigDecimal amount) {
                    if (balance == null || amount == null || balance.signum() < 0) return false;
                    if (amount.compareTo(BigDecimal.ZERO) <= 0) return false;
                    return amount.compareTo(balance) <= 0;
                }
                """;
        String response = response("""
                "summary":"All stated withdrawal requirements are met.",
                "strengths":["Uses numeric BigDecimal comparisons."],
                "issues":[],
                "improvements":[],
                "findings":[{"category":"ADDITIONAL_DEFENSIVE_CHECK","competency":"INPUT_VALIDATION","description":"Also rejects a negative available balance."}],
                "assessmentConfidence":"HIGH",
                "competencyTags":["INPUT_VALIDATION","DECIMAL_MONEY"],
                "nextActionType":"NEXT_LESSON",
                "nextAction":"Apply the same validation pattern to the next lesson.",
                "reviewRecommendation":"READY_FOR_REVIEW"
                """);

        AdvisoryFeedback assessment = assessSubmittedCode(code, response);

        assertThat(assessment.reviewRecommendation())
                .isEqualTo(AdvisoryFeedback.ReviewRecommendation.READY_FOR_REVIEW);
        assertThat(assessment.findings()).singleElement().satisfies(finding ->
                assertThat(finding.category())
                        .isEqualTo(ai.devpath.feedback.dto.FeedbackFinding.Category.ADDITIONAL_DEFENSIVE_CHECK));
    }

    @Test
    void missingNegativeAmountValidationIsAFunctionalDefect() throws Exception {
        String code = """
                public static boolean canWithdraw(BigDecimal balance, BigDecimal amount) {
                    return balance != null && amount != null && amount.compareTo(balance) <= 0;
                }
                """;
        AdvisoryFeedback assessment = assessSubmittedCode(code, response("""
                "summary":"Negative requested amounts are accepted.",
                "strengths":["Uses BigDecimal."],
                "issues":["The requested amount is not checked for positivity."],
                "improvements":["Reject amounts less than or equal to zero."],
                "findings":[{"category":"FUNCTIONAL_DEFECT","competency":"INPUT_VALIDATION","description":"Reject zero and negative requested amounts as required."}],
                "assessmentConfidence":"HIGH",
                "competencyTags":["INPUT_VALIDATION"],
                "nextActionType":"TARGETED_CORRECTION",
                "nextAction":"Add a requestedAmount.compareTo(BigDecimal.ZERO) > 0 guard before comparing with the balance.",
                "reviewRecommendation":"NEEDS_REVIEW"
                """));

        assertThat(assessment.reviewRecommendation())
                .isEqualTo(AdvisoryFeedback.ReviewRecommendation.NEEDS_REVIEW);
        assertThat(assessment.findings()).extracting(finding -> finding.category().name())
                .containsExactly("FUNCTIONAL_DEFECT");
    }

    @Test
    void scaleSensitiveBigDecimalEqualsIsIdentifiedAsComparisonDefect() throws Exception {
        String code = """
                public static boolean canWithdraw(BigDecimal balance, BigDecimal amount) {
                    return balance != null && amount != null && amount.equals(balance);
                }
                """;
        AdvisoryFeedback assessment = assessSubmittedCode(code, response("""
                "summary":"The comparison rejects valid amounts with different scales.",
                "strengths":["Uses BigDecimal."],
                "issues":["equals compares both value and scale."],
                "improvements":["Use compareTo to enforce amount less than or equal to balance."],
                "findings":[{"category":"FUNCTIONAL_DEFECT","competency":"DECIMAL_MONEY","description":"BigDecimal.equals is scale-sensitive and does not implement the required less-than-or-equal comparison."}],
                "assessmentConfidence":"HIGH",
                "competencyTags":["DECIMAL_MONEY","BOUNDARY_CONDITIONS"],
                "nextActionType":"TARGETED_CORRECTION",
                "nextAction":"Replace equals with compareTo(balance) <= 0 after validating both inputs.",
                "reviewRecommendation":"NEEDS_REVIEW"
                """));

        assertThat(assessment.findings()).singleElement().satisfies(finding ->
                assertThat(finding.competency().name()).isEqualTo("DECIMAL_MONEY"));
    }

    @Test
    void missingNullGuardsAreIdentifiedAsInputHandlingDefect() throws Exception {
        String code = """
                public static boolean canWithdraw(BigDecimal balance, BigDecimal amount) {
                    return amount.compareTo(balance) <= 0;
                }
                """;
        AdvisoryFeedback assessment = assessSubmittedCode(code, response("""
                "summary":"Null inputs can cause an exception.",
                "strengths":["The monetary boundary uses compareTo."],
                "issues":["Neither argument is checked for null."],
                "improvements":["Return false if either argument is null."],
                "findings":[{"category":"FUNCTIONAL_DEFECT","competency":"NULL_HANDLING","description":"The exercise requires false for either null argument."}],
                "assessmentConfidence":"HIGH",
                "competencyTags":["NULL_HANDLING","INPUT_VALIDATION"],
                "nextActionType":"TARGETED_CORRECTION",
                "nextAction":"Add null checks for balance and amount before calling compareTo.",
                "reviewRecommendation":"NEEDS_REVIEW"
                """));

        assertThat(assessment.findings()).singleElement().satisfies(finding ->
                assertThat(finding.competency().name()).isEqualTo("NULL_HANDLING"));
    }

    @Test
    void lowConfidenceMustBeReportedAsUncertainWithoutInventingDefects() throws Exception {
        AdvisoryFeedback assessment = provider.parseFeedback(response("""
                "summary":"The submitted fragment does not include enough code to assess the method.",
                "strengths":["A method signature is present."],
                "issues":["The implementation body is omitted."],
                "improvements":["Submit the complete method for review."],
                "findings":[],
                "assessmentConfidence":"LOW",
                "competencyTags":["JAVA_METHODS"],
                "nextActionType":"CLARIFICATION",
                "nextAction":"Please provide the full method; this submission is too incomplete to assess.",
                "reviewRecommendation":"UNCERTAIN"
                """));

        assertThat(assessment.reviewRecommendation()).isEqualTo(AdvisoryFeedback.ReviewRecommendation.UNCERTAIN);
        assertThat(assessment.nextAction()).contains("Assessment is uncertain");
        assertThat(assessment.nextActionType())
                .isEqualTo(ai.devpath.feedback.dto.NextActionType.CLARIFICATION);
    }

    @Test
    void rejectsNeedsReviewWithoutAnExplicitFunctionalDefect() throws Exception {
        String content = response("""
                "summary":"The exercise is said to need review.",
                "strengths":["Uses a clear method."],
                "issues":["Consider renaming a local variable."],
                "improvements":["Use a clearer local variable name."],
                "findings":[{"category":"OPTIONAL_IMPROVEMENT","competency":"JAVA_METHODS","description":"A variable name could be clearer."}],
                "assessmentConfidence":"HIGH",
                "competencyTags":["JAVA_METHODS"],
                "nextActionType":"TARGETED_CORRECTION",
                "nextAction":"Rename the local variable.",
                "reviewRecommendation":"NEEDS_REVIEW"
                """);

        assertThatThrownBy(() -> provider.parseFeedback(content))
                .isInstanceOf(FeedbackProviderException.class)
                .hasMessage("AI feedback response failed validation");
    }

    @Test
    void ignoresMalformedAndUnsupportedLegacyTagsButValidatesExplicitFindings() throws Exception {
        String content = """
                {
                  "summary":"The implementation is ready for reinforcement.",
                  "strengths":["Uses numeric comparison."],
                  "issues":[],
                  "improvements":[],
                  "findings":[],
                  "assessmentConfidence":"HIGH",
                  "competencyTags":["INPUT_VALIDATION","UNSUPPORTED_TAG",{"tag":"BOUNDARY_CONDITIONS"}],
                  "nextActionType":"NEXT_LESSON",
                  "nextAction":"Try the transfer validation lesson.",
                  "reviewRecommendation":"READY_FOR_REVIEW"
                }
                """;

        AdvisoryFeedback result = provider.parseFeedback(envelope(content));

        assertThat(result.competencyTags()).containsExactly("INPUT_VALIDATION");
    }

    @Test
    void fillsOnlyNonAssessmentMetadataWhenReadingPersistedLegacyFeedback() throws Exception {
        AdvisoryFeedback legacy = OBJECT_MAPPER.readValue("""
                {
                  "summary":"Legacy feedback.",
                  "strengths":["A saved strength."],
                  "issues":["A saved issue."],
                  "improvements":["A saved improvement."],
                  "competencyTags":["INPUT_VALIDATION"],
                  "nextAction":"Add a guard.",
                  "reviewRecommendation":"NEEDS_REVIEW"
                }
                """, AdvisoryFeedback.class).withCompatibilityDefaults();

        assertThat(legacy.findings()).isEmpty();
        assertThat(legacy.assessmentConfidence())
                .isEqualTo(ai.devpath.feedback.dto.AssessmentConfidence.MEDIUM);
        assertThat(legacy.nextActionType())
                .isEqualTo(ai.devpath.feedback.dto.NextActionType.TARGETED_CORRECTION);
    }

    private static AdvisoryFeedback assessSubmittedCode(String code, String response) throws Exception {
        JsonNode request = OBJECT_MAPPER.readTree(
                provider.createRequestBody(CuratedLessonCatalog.startingLesson(), code));
        String systemPrompt = request.path("messages").get(0).path("content").asText();
        String userPrompt = request.path("messages").get(1).path("content").asText();
        assertThat(systemPrompt)
                .contains("Assess only the exercise's explicit requirements")
                .contains("Additional validation that preserves all required valid cases is not")
                .contains("BigDecimal.equals is")
                .contains("scale-sensitive")
                .contains("ADDITIONAL_DEFENSIVE_CHECK")
                .contains("LOW, use UNCERTAIN");
        assertThat(userPrompt).contains(code)
                .contains("Return false when the requested amount is zero or negative.")
                .contains("Return false when either argument is null.")
                .contains("Balance 100.00, request 100.00 → true");
        return provider.parseFeedback(response);
    }

    private static String response(String fields) throws Exception {
        return envelope("{\n" + fields + "\n}");
    }

    private static String envelope(String content) throws Exception {
        var envelope = OBJECT_MAPPER.createObjectNode();
        envelope.putArray("choices")
                .addObject()
                .putObject("message")
                .put("content", content);
        return OBJECT_MAPPER.writeValueAsString(envelope);
    }
}
