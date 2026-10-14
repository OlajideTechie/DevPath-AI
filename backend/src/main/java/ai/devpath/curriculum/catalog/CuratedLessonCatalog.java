package ai.devpath.curriculum.catalog;

import java.util.List;

public final class CuratedLessonCatalog {
    private static final CuratedLesson STARTER_LESSON = new CuratedLesson(
            "java-methods-input-validation",
            "Java Methods and Input Validation",
            "Learn how Java methods organize business rules and how to validate input before processing a request.",
            "Write a focused Java method that rejects invalid withdrawal requests before changing an account balance.",
            20,
            "A banking API receives withdrawal requests. Validate each request before any balance update, just as you would verify API boundary cases in a QA test.",
            List.of(
                    new CuratedLesson.LessonSection(
                            "A method gives a rule a clear boundary",
                            "A Java method groups a focused operation behind a name, inputs, and a return value. Keeping validation in a small method makes the rule easier to read and test independently.",
                            """
                            boolean canWithdraw(BigDecimal availableBalance, BigDecimal requestedAmount) {
                                return requestedAmount.compareTo(availableBalance) <= 0;
                            }
                            """),
                    new CuratedLesson.LessonSection(
                            "Validate before comparing",
                            "Check that values exist and that the requested amount is positive before comparing it with the available balance. BigDecimal represents decimal money without binary floating-point rounding surprises.",
                            """
                            requestedAmount != null
                                && requestedAmount.compareTo(BigDecimal.ZERO) > 0
                            """),
                    new CuratedLesson.LessonSection(
                            "Compare monetary values by value",
                            "BigDecimal.compareTo compares numeric value. Unlike equals, it treats values such as 10.0 and 10.00 as numerically equal. A request equal to the available balance is valid for this exercise.",
                            """
                            requestedAmount.compareTo(availableBalance) <= 0
                            """)),
            new CuratedLesson.PracticeExercise(
                    "Validate a banking withdrawal",
                    "Complete canWithdraw so it accepts a withdrawal only when both values are present, the requested amount is positive, and it does not exceed the available balance. Do not change the balance in this method.",
                    List.of(
                            "Use BigDecimal for both monetary values; do not use float or double.",
                            "Return false when either argument is null.",
                            "Return false when the requested amount is zero or negative.",
                            "Return false when the requested amount is greater than the available balance.",
                            "Return true when the requested amount is less than or equal to the available balance.",
                            "Do not mutate the available balance."),
                    List.of(
                            "Balance 100.00, request 25.00 → true",
                            "Balance 100.00, request 100.00 → true",
                            "Balance 100.00, request 100.01 → false",
                            "Balance 100.00, request 0.00 → false",
                            "Balance 100.00, request -1.00 → false",
                            "Null balance or null request → false"),
                    """
                    import java.math.BigDecimal;

                    public class WithdrawalValidator {
                        public static boolean canWithdraw(
                                BigDecimal availableBalance,
                                BigDecimal requestedAmount) {
                            // TODO: Add withdrawal validation.
                            return false;
                        }
                    }
                    """));

    private static final CuratedLesson RECALL_LESSON = new CuratedLesson(
            "bank-transfer-validation-recall",
            "Recall: Validate a Bank Transfer",
            "Apply input validation and monetary boundary checks to a related banking workflow.",
            "Validate a transfer between two accounts without changing either balance.",
            15,
            "A transfer API must validate both account balances and the requested amount before processing. This changes the banking scenario from withdrawal to transfer.",
            List.of(
                    new CuratedLesson.LessonSection(
                            "Transfer rules span two accounts",
                            "A transfer has an additional boundary: the source must cover the amount and the destination account must be present. Validate all inputs before any balance changes.",
                            "sourceBalance != null && destinationBalance != null"),
                    new CuratedLesson.LessonSection(
                            "Validate the amount before comparing",
                            "Reject a missing, zero, or negative amount. Use BigDecimal comparisons for monetary values.",
                            "amount.compareTo(BigDecimal.ZERO) > 0"),
                    new CuratedLesson.LessonSection(
                            "Keep validation separate from mutation",
                            "This exercise only decides whether a transfer is valid. It must not mutate either account balance.",
                            "amount.compareTo(sourceBalance) <= 0")),
            new CuratedLesson.PracticeExercise(
                    "Validate a bank transfer",
                    "Complete canTransfer so a transfer is accepted only when the source balance, destination balance, and amount are present, both balances are non-negative, the amount is positive, and the amount does not exceed the source balance. Do not mutate either balance.",
                    List.of(
                            "Use BigDecimal for all monetary values.",
                            "Return false if any argument is null.",
                            "Return false if either balance is negative.",
                            "Return false if the requested amount is zero or negative.",
                            "Return false if the requested amount exceeds the source balance.",
                            "Return true when the amount is less than or equal to the source balance.",
                            "Do not mutate either balance."),
                    List.of(
                            "Source 100.00, destination 25.00, request 25.00 → true",
                            "Source 100.00, destination 25.00, request 100.00 → true",
                            "Source 100.00, destination 25.00, request 100.01 → false",
                            "Null source, destination, or amount → false",
                            "Negative balance, zero amount, or negative amount → false"),
                    """
                    import java.math.BigDecimal;

                    public class TransferValidator {
                        public static boolean canTransfer(
                                BigDecimal sourceBalance,
                                BigDecimal destinationBalance,
                                BigDecimal amount) {
                            // TODO: Add transfer validation.
                            return false;
                        }
                    }
                    """));

    private CuratedLessonCatalog() {
    }

    public static CuratedLesson startingLesson() {
        return STARTER_LESSON;
    }

    public static CuratedLesson recallLesson() {
        return RECALL_LESSON;
    }
}
