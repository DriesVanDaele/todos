package com.kuleuven.tagger;

/**
 * This class represents a Tag, with an associated name and confidence.
 */
public class Tag implements Comparable<Tag> {
	
	// an immutable String representing the name of the tag
	private final String tag;
	// an immutable double representing the confidence associated to the tag
	private final Double confidence;
	
	/**
	 * Initialize a tag with the given information
	 * @param tag : the name of the Tag
	 * @param confidence : the confidence associated with the tag
	 */
	public Tag (String tag, double confidence) {
		this.tag = tag;
		this.confidence = confidence;
	}
	
	public String getTag() {
		return tag.replaceFirst("TAG", "#");
	}
	
	public Double getConfidence() {
		return confidence;
	}
	
	public String toJSON() {
		return "{ \"name\": \"" + getTag() + "\", \"prob\": " + getConfidence() + " }";
	}
	
	@Override
	public String toString() {
		return getTag() + " " + getConfidence();
	}
	
	@Override
	public int compareTo(Tag other) {
		return getConfidence().compareTo(other.getConfidence());
	}
}
