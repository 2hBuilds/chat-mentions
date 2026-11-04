package com.chatmentions;

import javax.swing.text.AbstractDocument;
import javax.swing.text.Element;
import javax.swing.text.LabelView;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * A styled editor kit whose text never asks for more width than the line has. Since Java 7 a word longer than the line
 * is no longer broken on its own: the text view's minimum width becomes that word's width, and a text pane in a scroll
 * pane stops following the viewport, so every line wraps at the one long word's width and clips under the scrollbar.
 *
 * <p>Here a content element gets a {@link LabelView} whose minimum span on the X axis is 0, so a word longer than the
 * line breaks between characters onto the next line; words that fit still break at spaces. Icons and components keep
 * the default views. Set the kit before any document is set on the pane: setting a kit replaces the document.
 */
final class WrapKit extends StyledEditorKit
{
	private final ViewFactory factory = new WrapFactory(super.getViewFactory());

	@Override
	public ViewFactory getViewFactory()
	{
		return factory;
	}

	/** The default views, except that the text runs may be broken anywhere. */
	private static final class WrapFactory implements ViewFactory
	{
		private final ViewFactory base;

		WrapFactory(ViewFactory base)
		{
			this.base = base;
		}

		@Override
		public View create(Element elem)
		{
			if (AbstractDocument.ContentElementName.equals(elem.getName()))
			{
				return new BreakableLabel(elem);
			}
			return base.create(elem);
		}
	}

	/** A text run that can shrink to nothing on the X axis, so the line can break inside a long word. */
	private static final class BreakableLabel extends LabelView
	{
		BreakableLabel(Element elem)
		{
			super(elem);
		}

		@Override
		public float getMinimumSpan(int axis)
		{
			return axis == X_AXIS ? 0f : super.getMinimumSpan(axis);
		}
	}
}
