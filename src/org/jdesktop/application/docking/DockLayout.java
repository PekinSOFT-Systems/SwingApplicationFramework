/*
 * Copyright (C) 2021 PekinSOFT Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * *****************************************************************************
 * Project    :   Northwind
 * Class      :   DockLayout.java
 * Author     :   Sean Carrick <sean at pekinsoft dot com>
 * Created    :   Jan 4, 2021 @ 9:58:10 AM
 * Modified   :   Jan 4, 2021
 *  
 * Purpose:
 * 	
 * Revision History:
 *  
 * WHEN          BY                  REASON
 * ------------  ------------------- -------------------------------------------
 * Jan 4, 2021     Sean Carrick             Initial creation.
 * *****************************************************************************
 */

package org.jdesktop.application.docking;

import org.jdesktop.application.docking.DockConstraints.DockLocation;
import static org.jdesktop.application.docking.DockConstraints.DockLocation.EAST;
import static org.jdesktop.application.docking.DockConstraints.DockLocation.NORTH;
import static org.jdesktop.application.docking.DockConstraints.DockLocation.SOUTH;
import static org.jdesktop.application.docking.DockConstraints.DockLocation.WEST;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Sean Carrick &lt;sean at pekinsoft dot com&gt;
 * 
 * @version 0.1.0
 * @since 0.1.0
 */
public class DockLayout implements LayoutManager2 {

	private Layoutable root;
	private Map<Component, LayoutableComponent> layoutableComponents = new HashMap<Component, LayoutableComponent>();

	public void layoutContainer(Container parent) {
		Insets insets = parent.getInsets();
		layout(insets.left, insets.top, parent.getWidth() - insets.left
               - insets.right, parent.getHeight() - insets.top - insets.bottom);
	}

	protected void layout(int x, int y, int width, int height) {
		if (root != null) {
			root.layout(x, y, width, height);
		}
	}
	
	public boolean containsLayoutComponent(Component component) {
		return layoutableComponents.containsKey(component);
	}

	public void addLayoutComponent(Component component, Object constraints) {
		if (containsLayoutComponent(component)) {
			if (constraints == null) {
				return;
			}
			removeLayoutComponent(component);
		}
		DockConstraints dockConstraints = null;
		if (constraints instanceof DockConstraints) {
			dockConstraints = (DockConstraints) constraints;
		}
		LayoutableComponent child = new LayoutableComponent(component);
		if (root == null) {
			root = child;
			return;
		}
		if (constraints == null) {
			root = new HierarchicalLayoutable(root, child);
			return;
		}
		if (component == dockConstraints.getComponentToDockAt()) {
			throw new IllegalArgumentException("component cannot be docked to itself");
		}
		LayoutableComponent sibling = layoutableComponents.get(dockConstraints.getComponentToDockAt());
		if (sibling == null) {
			throw new IllegalArgumentException("componentToDock was not added to this layout");
		}
		HierarchicalLayoutable oldParent = sibling.getParent();
		HierarchicalLayoutable newParent
			= createParent(child, sibling,	dockConstraints.getDockLocation(), dockConstraints.getAlignment());
		if (sibling.isRoot()) {
			root = newParent;
			return;
		}
		if (oldParent.getChild1() == null) {
			oldParent.setChild1(newParent);
		} else if (oldParent.getChild2() == null) {
			oldParent.setChild2(newParent);
		}
	}

	public void addLayoutComponent(String name, Component component) {
		addLayoutComponent(component, null);
	}

	public void removeLayoutComponent(Component component) {
		try {
			LayoutableComponent child = layoutableComponents.get(component);
			if (child == null) {
				return;
			}
			if (child.isRoot()) {
				root = null;
				return;
			}
			Layoutable sibling;
			if (child.getParent().getChild1() == child) {
				sibling = child.getParent().getChild2();
			} else if (child.getParent().getChild2() == child) {
				sibling = child.getParent().getChild1();
			} else {
				throw new IllegalStateException("Layout-tree corrupted");
			}
			if (child.getParent().isRoot()) {
				root = sibling;
				root.removeParent();
				return;
			}
			HierarchicalLayoutable newParent = child.getParent().getParent();
			if (newParent.getChild1() == child.getParent()) {
				newParent.setChild1(sibling);
			} else if (newParent.getChild2() == child.getParent()) {
				newParent.setChild2(sibling);
			} else {
				throw new IllegalStateException("Layout-tree corrupted");
			}
		} finally {
			layoutableComponents.remove(component);
		}
	}
	
	public void replaceLayoutComponent(Component oldComponent, Component newComponent) {
		LayoutableComponent child = layoutableComponents.get(oldComponent);
		if (child == null) {
			return;
		}
		child.component = newComponent;
		layoutableComponents.remove(oldComponent);
		layoutableComponents.put(newComponent, child);
		layoutContainer(oldComponent.getParent());
	}

	public void resizeLayoutComponent(Component component, int x, int y,
			int width, int height) {
		LayoutableComponent layoutableComponent = layoutableComponents
				.get(component);
		if (layoutableComponent != null) {
			if (component.getY() != y) {
				resizeTop(layoutableComponent, component.getY() - y);
				layoutContainer(component.getParent());
			}
			if (component.getX() != x) {
				resizeLeft(layoutableComponent, component.getX() - x);
				layoutContainer(component.getParent());
			}
			if (component.getY() + component.getHeight() != y + height) {
				resizeBottom(layoutableComponent, component.getY()
						+ component.getHeight() - y - height);
				layoutContainer(component.getParent());
			}
			if (component.getX() + component.getWidth() != x + width) {
				resizeRight(layoutableComponent, component.getX()
						+ component.getWidth() - x - width);
				layoutContainer(component.getParent());
			}
		}
	}

	public void maximizeLayoutComponent(Component component) {
		LayoutableComponent layoutableComponent = layoutableComponents.get(component);
		if (layoutableComponent == null) {
			throw new IllegalArgumentException("The specified component is not contained in the layout");
		}
		hide(root);
		layoutableComponent.setVisible(true);
		layoutContainer(component.getParent());
	}

	public void minimizeLayoutComponent(Component component) {
		LayoutableComponent layoutableComponent = layoutableComponents.get(component);
		if (layoutableComponent == null) {
			return;
		}
		show(root);
		layoutContainer(component.getParent());
	}

	private void hide(Layoutable layoutable) {
		if (layoutable instanceof LayoutableComponent) {
			((LayoutableComponent)layoutable).setVisible(false);
		} else if (layoutable instanceof HierarchicalLayoutable) {
			hide(((HierarchicalLayoutable) layoutable).getChild1());
			hide(((HierarchicalLayoutable) layoutable).getChild2());
		} else {
			throw new IllegalArgumentException(layoutable == null?
					                           "layoutable must not be null":
											   "layoutable-type not supported" + layoutable.getClass().getName());
		}
	}

	private void show(Layoutable layoutable) {
		if (layoutable instanceof LayoutableComponent) {
			((LayoutableComponent) layoutable).setVisible(true);
		} else if (layoutable instanceof HierarchicalLayoutable) {
			show(((HierarchicalLayoutable) layoutable).getChild1());
			show(((HierarchicalLayoutable) layoutable).getChild2());
		} else {
			throw new IllegalArgumentException(
					layoutable == null ? "layoutable must not be null"
							: "layoutable-type not supported"
									+ layoutable.getClass().getName());
		}
	}

	private void resizeTop(Layoutable layoutable, int amount) {
		HierarchicalLayoutable parent = layoutable.getParent();
		if (parent == null) {
			return;
		}
		if (parent.getOrientation() == SplitOrientation.HORIZONTAL) {
			int parentHeight = parent.getActualLayoutHeight();
			int newHeight = layoutable.getActualLayoutHeight() + amount;
			if (parent.getChild1() == layoutable) {
				resizeTop(parent, amount);
				parentHeight += amount;
				parent.setAlignment((float) newHeight / parentHeight);
			} else if (parent.getChild2() == layoutable) {
				parent.setAlignment((float) (parentHeight - newHeight)
						/ parentHeight);
			} else {
				throw new IllegalStateException("Layout-tree corrupted");
			}
		} else {
			resizeTop(parent, amount);
		}
	}

	private void resizeLeft(Layoutable layoutable, int amount) {
		HierarchicalLayoutable parent = layoutable.getParent();
		if (parent == null) {
			return;
		}
		if (parent.getOrientation() == SplitOrientation.VERTICAL) {
			int parentWidth = parent.getActualLayoutWidth();
			int newWidth = layoutable.getActualLayoutWidth() + amount;
			if (parent.getChild1() == layoutable) {
				resizeLeft(parent, amount);
				parentWidth += amount;
				parent.setAlignment((float) newWidth / parentWidth);
			} else if (parent.getChild2() == layoutable) {
				parent.setAlignment((float) (parentWidth - newWidth)
						/ parentWidth);
			} else {
				throw new IllegalStateException("Layout-tree corrupted");
			}
		} else {
			resizeLeft(parent, amount);
		}
	}

	private void resizeBottom(Layoutable layoutable, int amount) {
		HierarchicalLayoutable parent = layoutable.getParent();
		if (parent == null) {
			return;
		}
		if (parent.getOrientation() == SplitOrientation.HORIZONTAL) {
			int parentHeight = parent.getActualLayoutHeight();
			int newHeight = layoutable.getActualLayoutHeight() - amount;
			if (parent.getChild1() == layoutable) {
				parent.setAlignment((float) newHeight / parentHeight);
			} else if (parent.getChild2() == layoutable) {
				resizeBottom(parent, amount);
				parentHeight -= amount;
				parent.setAlignment((float) (parentHeight - newHeight)
						/ parentHeight);
			} else {
				throw new IllegalStateException("Layout-tree corrupted");
			}
		} else {
			resizeBottom(parent, amount);
		}
	}

	private void resizeRight(Layoutable layoutable, int amount) {
		HierarchicalLayoutable parent = layoutable.getParent();
		if (parent == null) {
			return;
		}
		if (parent.getOrientation() == SplitOrientation.VERTICAL) {
			int parentWidth = parent.getActualLayoutWidth();
			int newWidth = layoutable.getActualLayoutWidth() - amount;
			if (parent.getChild1() == layoutable) {
				parent.setAlignment((float) newWidth / parentWidth);
			} else if (parent.getChild2() == layoutable) {
				resizeRight(parent, amount);
				parentWidth -= amount;
				parent.setAlignment((float) (parentWidth - newWidth)
						/ parentWidth);
			} else {
				throw new IllegalStateException("Layout-tree corrupted");
			}
		} else {
			resizeRight(parent, amount);
		}
	}

	private HierarchicalLayoutable createParent(LayoutableComponent child,
                                                LayoutableComponent sibling,
                                                DockLocation dockLocation,
                                                float alignment) {
		switch (dockLocation) {
		case NORTH:
			return new HierarchicalLayoutable(child, sibling, SplitOrientation.HORIZONTAL, alignment);
		case SOUTH:
			return new HierarchicalLayoutable(sibling, child, SplitOrientation.HORIZONTAL, alignment);
		case EAST:
			return new HierarchicalLayoutable(sibling, child, SplitOrientation.VERTICAL, alignment);
		case WEST:
			return new HierarchicalLayoutable(child, sibling, SplitOrientation.VERTICAL, alignment);
		}
		throw new IllegalStateException("dockLocation is none of NORTH, SOUTH, EAST, WEST");
	}

	public Dimension minimumLayoutSize(Container parent) {
		return root == null ? new Dimension() : new Dimension(root
				.getMinimumLayoutWidth(), root.getMinimumLayoutHeight());
	}

	public Dimension preferredLayoutSize(Container parent) {
		return root == null ? new Dimension() : new Dimension(root
				.getPreferredLayoutWidth(), root.getPreferredLayoutHeight());
	}

	public Dimension maximumLayoutSize(Container target) {
		return root == null ? new Dimension(Integer.MAX_VALUE,
				Integer.MAX_VALUE) : new Dimension(
				root.getMaximumLayoutWidth(), root.getMaximumLayoutHeight());
	}

	public float getLayoutAlignmentX(Container target) {
		return 0.5f;
	}

	public float getLayoutAlignmentY(Container target) {
		return 0.5f;
	}

	public void invalidateLayout(Container target) {
	}

	private interface Layoutable {

		boolean isRoot();
		
		HierarchicalLayoutable getParent();
		
		void removeParent();

		boolean isVisible();

		int getActualLayoutWidth();

		int getActualLayoutHeight();

		int getMinimumLayoutWidth();

		int getMinimumLayoutHeight();

		int getPreferredLayoutWidth();

		int getPreferredLayoutHeight();

		int getMaximumLayoutWidth();

		int getMaximumLayoutHeight();

		void layout(int x, int y, int width, int height);
	}

	private abstract class AbstractLayoutable implements Layoutable {

		private HierarchicalLayoutable parent;

		public boolean isRoot() {
			return root == this;
		}
		
		public HierarchicalLayoutable getParent() {
			return parent;
		}
		
		public void removeParent() {
			if (!isRoot()) {
				throw new IllegalStateException("Only the root node may have no parent");
			}
			parent = null;
		}
	}

	private class LayoutableComponent extends AbstractLayoutable {

		private Component component;
		private boolean visible = true;

		public LayoutableComponent(Component component) {
			this.component = component;
			layoutableComponents.put(component, this);
		}

		public boolean isVisible() {
			return component.isVisible() && visible;
		}

		public void setVisible(boolean visible) {
			this.visible = visible;
		}

		public int getActualLayoutWidth() {
			return component.getWidth();
		}

		public int getActualLayoutHeight() {
			return component.getHeight();
		}

		public int getMinimumLayoutHeight() {
			return component.getMinimumSize().height;
		}

		public int getMinimumLayoutWidth() {
			return component.getMinimumSize().width;
		}

		public int getPreferredLayoutHeight() {
			return component.getPreferredSize().height;
		}

		public int getPreferredLayoutWidth() {
			return component.getPreferredSize().width;
		}

		public int getMaximumLayoutHeight() {
			return component.getMaximumSize().height;
		}

		public int getMaximumLayoutWidth() {
			return component.getMaximumSize().width;
		}

		public void layout(int x, int y, int width, int height) {
			component.setBounds(x, y, width, height);
		}
	}

	private class HierarchicalLayoutable extends AbstractLayoutable {

		private SplitOrientation orientation;
		private float alignment;
		private Layoutable child1;
		private Layoutable child2;

		public HierarchicalLayoutable(Layoutable child1, Layoutable child2) {
			this(child1, child2, SplitOrientation.VERTICAL);
		}

		public HierarchicalLayoutable(Layoutable child1, Layoutable child2,	SplitOrientation orientation) {
			this(child1, child2, orientation, 0.5f);
		}

		public HierarchicalLayoutable(Layoutable child1, Layoutable child2,	SplitOrientation orientation, float alignment) {
			this.orientation = orientation;
			this.alignment = alignment;
			setChild1(child1);
			setChild2(child2);
		}

		public boolean isVisible() {
			return child1.isVisible() || child2.isVisible();
		}

		public SplitOrientation getOrientation() {
			return orientation;
		}

		public float getAlignment() {
			return alignment;
		}

		public void setAlignment(float alignment) {
			if (alignment < 0) {
				alignment = 0;
			}
			if (alignment > 1) {
				alignment = 1;
			}
			this.alignment = alignment;
		}

		public int getActualLayoutWidth() {
			if (child1.isVisible() && child2.isVisible()) {
				switch (orientation) {
				case HORIZONTAL:
					return Math.max(child1.getActualLayoutWidth(), child2
							.getActualLayoutWidth());
				case VERTICAL:
					return child1.getActualLayoutWidth()
							+ child2.getActualLayoutWidth();
				}
				throw new NullPointerException("orientation == null");
			} else if (child1.isVisible()) {
				return child1.getActualLayoutWidth();
			} else if (child2.isVisible()) {
				return child2.getActualLayoutWidth();
			} else {
				return 0;
			}
		}

		public int getActualLayoutHeight() {
			if (child1.isVisible() && child2.isVisible()) {
				switch (orientation) {
				case HORIZONTAL:
					return child1.getActualLayoutHeight()
							+ child2.getActualLayoutHeight();
				case VERTICAL:
					return Math.max(child1.getActualLayoutHeight(), child2
							.getActualLayoutHeight());
				}
				throw new NullPointerException("orientation == null");
			} else if (child1.isVisible()) {
				return child1.getActualLayoutHeight();
			} else if (child2.isVisible()) {
				return child2.getActualLayoutHeight();
			} else {
				return 0;
			}
		}

		public int getMinimumLayoutWidth() {
			if (child1.isVisible() && child2.isVisible()) {
				switch (orientation) {
				case HORIZONTAL:
					return Math.min(child1.getMinimumLayoutWidth(), child2
							.getMinimumLayoutWidth());
				case VERTICAL:
					return child1.getMinimumLayoutWidth()
							+ child2.getMinimumLayoutWidth();
				}
				throw new NullPointerException("orientation == null");
			} else if (child1.isVisible()) {
				return child1.getMinimumLayoutWidth();
			} else if (child2.isVisible()) {
				return child2.getMinimumLayoutWidth();
			} else {
				return 0;
			}
		}

		public int getMinimumLayoutHeight() {
			if (child1.isVisible() && child2.isVisible()) {
				switch (orientation) {
				case HORIZONTAL:
					return child1.getMinimumLayoutHeight()
							+ child2.getMinimumLayoutHeight();
				case VERTICAL:
					return Math.min(child1.getMinimumLayoutHeight(), child2
							.getMinimumLayoutHeight());
				}
				throw new NullPointerException("orientation == null");
			} else if (child1.isVisible()) {
				return child1.getMinimumLayoutHeight();
			} else if (child2.isVisible()) {
				return child2.getMinimumLayoutHeight();
			} else {
				return 0;
			}
		}

		public int getPreferredLayoutWidth() {
			if (child1.isVisible() && child2.isVisible()) {
				switch (orientation) {
				case HORIZONTAL:
					return Math.max(child1.getPreferredLayoutWidth(), child2
							.getPreferredLayoutWidth());
				case VERTICAL:
					return child1.getPreferredLayoutWidth()
							+ child2.getPreferredLayoutWidth();
				}
				throw new NullPointerException("orientation == null");
			} else if (child1.isVisible()) {
				return child1.getPreferredLayoutWidth();
			} else if (child2.isVisible()) {
				return child2.getPreferredLayoutWidth();
			} else {
				return 0;
			}
		}

		public int getPreferredLayoutHeight() {
			if (child1.isVisible() && child2.isVisible()) {
				switch (orientation) {
				case HORIZONTAL:
					return child1.getPreferredLayoutHeight()
							+ child2.getPreferredLayoutHeight();
				case VERTICAL:
					return Math.max(child1.getPreferredLayoutHeight(), child2
							.getPreferredLayoutHeight());
				}
				throw new NullPointerException("orientation == null");
			} else if (child1.isVisible()) {
				return child1.getPreferredLayoutHeight();
			} else if (child2.isVisible()) {
				return child2.getPreferredLayoutHeight();
			} else {
				return 0;
			}
		}

		public int getMaximumLayoutWidth() {
			if (child1.isVisible() && child2.isVisible()) {
				switch (orientation) {
				case HORIZONTAL:
					return Math.max(child1.getMaximumLayoutWidth(), child2
							.getMaximumLayoutWidth());
				case VERTICAL:
					return child1.getMaximumLayoutWidth()
							+ child2.getMaximumLayoutWidth();
				}
				throw new NullPointerException("orientation == null");
			} else if (child1.isVisible()) {
				return child1.getMaximumLayoutWidth();
			} else if (child2.isVisible()) {
				return child2.getMaximumLayoutWidth();
			} else {
				return 0;
			}
		}

		public int getMaximumLayoutHeight() {
			if (child1.isVisible() && child2.isVisible()) {
				switch (orientation) {
				case HORIZONTAL:
					return child1.getMaximumLayoutHeight()
							+ child2.getMaximumLayoutHeight();
				case VERTICAL:
					return Math.max(child1.getMaximumLayoutHeight(), child2
							.getMaximumLayoutHeight());
				}
				throw new NullPointerException("orientation == null");
			} else if (child1.isVisible()) {
				return child1.getMaximumLayoutHeight();
			} else if (child2.isVisible()) {
				return child2.getMaximumLayoutHeight();
			} else {
				return 0;
			}
		}

		public Layoutable getChild1() {
			return child1;
		}

		public void setChild1(Layoutable child) {
			if (child1 instanceof AbstractLayoutable) {
				((AbstractLayoutable) child1).parent = null;
			}
			child1 = child;
			if (child1 instanceof AbstractLayoutable) {
				AbstractLayoutable newChild = (AbstractLayoutable) child1;
				if (newChild.parent != null) {
					if (newChild.parent.child1 == newChild) {
						newChild.parent.setChild1(null);
					} else if (newChild.parent.child2 == newChild) {
						newChild.parent.setChild2(null);
					}
				}
				newChild.parent = this;
			}
		}

		public Layoutable getChild2() {
			return child2;
		}

		public void setChild2(Layoutable child) {
			if (child2 instanceof AbstractLayoutable) {
				((AbstractLayoutable) child2).parent = null;
			}
			child2 = child;
			if (child2 instanceof AbstractLayoutable) {
				AbstractLayoutable newChild = (AbstractLayoutable) child2;
				if (newChild.parent != null) {
					if (newChild.parent.child1 == newChild) {
						newChild.parent.setChild1(null);
					} else if (newChild.parent.child2 == newChild) {
						newChild.parent.setChild2(null);
					}
				}
				newChild.parent = this;
			}
		}

		public void layout(int x, int y, int width, int height) {
			if (child1.isVisible() && child2.isVisible()) {
				switch (orientation) {
				case HORIZONTAL:
					int height1 = (int) (height * alignment);
					int height2 = height - height1;
					child1.layout(x, y, width, height1);
					child2.layout(x, y + height1, width, height2);
					break;
				case VERTICAL:
					int width1 = (int) (width * alignment);
					int width2 = width - width1;
					child1.layout(x, y, width1, height);
					child2.layout(x + width1, y, width2, height);
					break;
				}
			} else if (child1.isVisible()) {
				child1.layout(x, y, width, height);
			} else if (child2.isVisible()) {
				child2.layout(x, y, width, height);
			}
		}
	}

	private enum SplitOrientation {
		HORIZONTAL, VERTICAL;
	}
}
