package se.gustavkarlsson.rocketchat.jira_trigger.messages;

import com.atlassian.jira.rest.client.api.domain.BasicPriority;
import com.atlassian.jira.rest.client.api.domain.Issue;
import se.gustavkarlsson.rocketchat.jira_trigger.messages.fields.FieldExtractor;
import se.gustavkarlsson.rocketchat.models.to_rocket_chat.Field;
import se.gustavkarlsson.rocketchat.models.to_rocket_chat.ToRocketChatAttachment;

import javax.ws.rs.core.UriBuilder;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.commons.lang.StringEscapeUtils.unescapeHtml;
import static org.apache.commons.lang3.Validate.noNullElements;
import static org.apache.commons.lang3.Validate.notNull;

public class AttachmentConverter {
	static final String BLOCKER_COLOR = "#FF4437";
	static final String CRITICAL_COLOR = "#D04437";
	static final String MAJOR_COLOR = "#E3833C";
	static final String MINOR_COLOR = "#F6C342";
	static final String TRIVIAL_COLOR = "#707070";

	private final boolean priorityColors;
	private final String defaultColor;
	private final List<FieldExtractor> defaultFieldExtractors;
	private final List<FieldExtractor> extendedFieldExtractors;

	AttachmentConverter(boolean priorityColors, String defaultColor, List<FieldExtractor> defaultFieldExtractors, List<FieldExtractor> extendedFieldExtractors) {
		this.priorityColors = priorityColors;
		this.defaultColor = notNull(defaultColor);
		this.defaultFieldExtractors = noNullElements(defaultFieldExtractors);
		this.extendedFieldExtractors = noNullElements(extendedFieldExtractors);
	}

	public ToRocketChatAttachment convert(Issue issue, Boolean extended) {
		List<FieldExtractor> fieldExtractors = extended ? extendedFieldExtractors : defaultFieldExtractors;
		return createAttachment(issue, fieldExtractors);
	}

	private ToRocketChatAttachment createAttachment(Issue issue, List<FieldExtractor> fieldExtractors) {
		ToRocketChatAttachment attachment = new ToRocketChatAttachment();
		attachment.setTitle(issue.getKey());
		if (priorityColors && issue.getPriority() != null) {
			attachment.setColor(getPriorityColor(issue.getPriority(), defaultColor));
		} else {
			attachment.setColor(defaultColor);
		}
		attachment.setText(createSummaryLink(issue));
		List<Field> fields = fieldExtractors.stream().map(fc -> fc.create(issue)).collect(Collectors.toList());
		attachment.setFields(fields);
		return attachment;
	}

	private String createSummaryLink(Issue issue) {
		String summary = Optional.ofNullable(issue.getSummary()).orElse("");
		String unescaped = unescapeHtml(summary);
		String stripped = stripReservedLinkCharacters(unescaped);
		return String.format("<%s|%s>", parseTitleLink(issue), stripped);
	}

	private static String stripReservedLinkCharacters(String text) {
		return text.replaceAll("[<>]", "");
	}

	private String parseTitleLink(Issue issue) {
		return UriBuilder.fromUri(issue.getSelf())
				.replacePath(null)
				.path("browse/")
				.path(issue.getKey())
				.build()
				.toASCIIString();
	}

	private String getPriorityColor(BasicPriority priority, String fallbackColor) {
		switch (priority.getName()) {
			case "Blocker":
				return BLOCKER_COLOR;
			case "Critical":
				return CRITICAL_COLOR;
			case "Major":
				return MAJOR_COLOR;
			case "Minor":
				return MINOR_COLOR;
			case "Trivial":
				return TRIVIAL_COLOR;
			default:
				return fallbackColor;
		}
	}

}
