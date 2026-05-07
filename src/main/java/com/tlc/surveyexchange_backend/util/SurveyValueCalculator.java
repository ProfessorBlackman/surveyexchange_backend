package com.tlc.surveyexchange_backend.util;

import com.tlc.surveyexchange_backend.model.QuestionType;

import java.util.List;
import java.util.Map;

public final class SurveyValueCalculator {

    // Credits per question type. 1 credit = 1 cedis.
    private static final Map<QuestionType, Integer> CREDIT_PER_TYPE = Map.of(
            QuestionType.MULTIPLE_CHOICE, 1,
            QuestionType.CHECKBOX,        1,
            QuestionType.SHORT_TEXT,      2,
            QuestionType.RATING,          1,
            QuestionType.YES_NO,          1,
            QuestionType.LONG_TEXT,       3
    );

    private SurveyValueCalculator() {}

    public static int calculate(List<QuestionType> questionTypes) {
        return questionTypes.stream()
                .mapToInt(type -> CREDIT_PER_TYPE.getOrDefault(type, 0))
                .sum();
    }
}
