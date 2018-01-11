package com.ecg.replyts.bolt.filter.user;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class BoltUserFilterConfig {
    private String kernelApiUrl;
    
    private Set<String> blackList = new HashSet<>();
    
    private Set<Pattern> blackListEmailPattern = new HashSet<>();

    private Integer blockUserScore;
    private Integer blackListUserScore;

    private Integer newUserHours;
    private Integer newUserScore;

    private Set<Pattern> blackListUserNamePattern = new HashSet<>();

    public String getKernelApiUrl() {
        return kernelApiUrl;
    }

    public void setKernelApiUrl(String kernelApiUrl) {
        this.kernelApiUrl = kernelApiUrl;
    }

    public Set<String> getBlackList() {
        return blackList;
    }

    public void setBlackList(Set<String> blackList) {
        this.blackList = blackList;
    }

    public Set<Pattern> getBlackListEmailPattern() {
        return blackListEmailPattern;
    }

    public void setBlackListEmailPattern(Set<Pattern> blackListEmailPattern) {
        this.blackListEmailPattern = blackListEmailPattern;
    }

    public Integer getBlockUserScore() {
        return blockUserScore;
    }

    public void setBlockUserScore(Integer blockUserScore) {
        this.blockUserScore = blockUserScore;
    }

    public Integer getBlackListUserScore() {
        return blackListUserScore;
    }

    public void setBlackListUserScore(Integer blackListUserScore) {
        this.blackListUserScore = blackListUserScore;
    }

    public Integer getNewUserHours() {
        return newUserHours;
    }

    public void setNewUserHours(Integer newUserHours) {
        this.newUserHours = newUserHours;
    }

    public Integer getNewUserScore() {
        return newUserScore;
    }

    public void setNewUserScore(Integer newUserScore) {
        this.newUserScore = newUserScore;
    }

    public Set<Pattern> getBlackListUserNamePattern() {
        return blackListUserNamePattern;
    }

    public void setBlackListUserNamePattern(Set<Pattern> blackListUserNamePattern) {
        this.blackListUserNamePattern = blackListUserNamePattern;
    }

    public void addToBlackList(String email){
        blackList.add(email);
    }

    public void addToBlackListEmailPattern(Pattern pattern){
    	blackListEmailPattern.add(pattern);
    }
 
    public void addToBlackListUserNamePattern(Pattern pattern){
    	blackListUserNamePattern.add(pattern);
    }
}