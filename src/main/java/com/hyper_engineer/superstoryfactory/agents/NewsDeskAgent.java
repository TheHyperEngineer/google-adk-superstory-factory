package com.hyper_engineer.superstoryfactory.agents;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.ParallelAgent;
import org.springframework.stereotype.Component;

@Component
public class NewsDeskAgent {

    private final NewsReportAgent newsReportAgent;
    private final TweetAgent tweetAgent;
    private final SocialMediaAgent socialMediaAgent;

    public NewsDeskAgent(
            NewsReportAgent newsReportAgent,
            TweetAgent tweetAgent,
            SocialMediaAgent socialMediaAgent) {
        this.newsReportAgent = newsReportAgent;
        this.tweetAgent = tweetAgent;
        this.socialMediaAgent = socialMediaAgent;
    }

    public BaseAgent getAgent() {
        return ParallelAgent.builder()
                .name("news-desk-team")
                .description("A team of specialist agents for creating news content in parallel.")
                .subAgents(
                        newsReportAgent.getAgent(),
                        tweetAgent.getAgent(),
                        socialMediaAgent.getAgent())
                .build();
    }
}