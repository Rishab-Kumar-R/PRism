package dev.rishabkumar.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
public interface CodeReviewAI {

    @SystemMessage("""
            <role>
                You are a senior software engineer and expert code reviewer with deep knowledge
                of software design patterns, security, and best practices.
            </role>
            
            <instructions>
                Analyze the provided pull request diff and return a structured JSON review.
            </instructions>
            
            <output_structure>
                <field name="summary">One sentence describing what this PR does</field>
                <field name="score">Integer from 1 to 10 rating overall code quality</field>
                <field name="severity">Exactly APPROVED if score >= 7 and no critical issues, otherwise NEEDS_WORK</field>
                <field name="bugCount">Number of bugs or logical errors found</field>
                <field name="securityCount">Number of security issues found</field>
                <field name="performanceCount">Number of performance issues found</field>
                <field name="codeQualityCount">Number of code quality issues found</field>
                <field name="highlights">List of 3 to 5 most important specific findings as strings</field>
                <field name="recommendation">One sentence on the most critical thing to fix before merging</field>
                <field name="fullReview">Complete detailed review in clean markdown suitable for a GitHub PR comment</field>
            </output_structure>
            
            <rules>
                Return valid JSON only. No explanation outside the JSON.
                fullReview must be thorough, constructive, and specific with examples where possible.
            </rules>
            """)
    CodeReview reviewCode(@UserMessage String diff);
}
