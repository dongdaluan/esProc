package com.scudata.ide.spl.control;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Vector;

import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import com.scudata.cellset.INormalCell;
import com.scudata.cellset.datamodel.CellSet;
import com.scudata.cellset.datamodel.NormalCell;
import com.scudata.cellset.datamodel.PgmNormalCell;
import com.scudata.common.Area;
import com.scudata.common.CellLocation;
import com.scudata.common.StringUtils;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.IAtomicCmd;
import com.scudata.ide.common.control.CellRect;
import com.scudata.ide.spl.AtomicCell;
import com.scudata.ide.spl.AtomicSpl;
import com.scudata.ide.spl.GCSpl;
import com.scudata.ide.spl.GMSpl;
import com.scudata.ide.spl.GVSpl;
import com.scudata.ide.spl.SPL;
import com.scudata.ide.spl.SheetSpl;

/**
 * ��Ԫ��ѡ�������
 *
 */
public class CellSelectListener implements MouseMotionListener, MouseListener, KeyListener {
	/** �༭�ؼ� */
	private SplControl control;

	/** ������� */
	private ContentPanel cp;

	/**
	 * �Ƿ���Ա༭
	 */
	private boolean editable = true;

	/** ��������� */
	private final byte NEXT_TOP = 1;
	/** ��������� */
	private final byte NEXT_BOTTOM = 2;
	/** ��������� */
	private final byte NEXT_LEFT = 3;
	/** ��������� */
	private final byte NEXT_RIGHT = 4;

	/** ��ק���� */
	private final byte FORWARD_UP = 0x1;
	/** ��ק���� */
	private final byte FORWARD_DOWN = 0x2;
	/** ��ק���� */
	private final byte FORWARD_LEFT = 0x4;
	/** ��ק���� */
	private final byte FORWARD_RIGHT = 0x8;

	/**
	 * ��ק�Ķ�ʱ��
	 */
	private javax.swing.Timer dragTimer = null;

	/**
	 * �кź��к�
	 */
	private int row = 0, col = 0;

	/**
	 * ��ק����
	 */
	private byte forward = 0;

	/**
	 * ���������캯��
	 * 
	 * @param control �༭�ؼ�
	 * @param panel   �������
	 */
	public CellSelectListener(SplControl control, ContentPanel panel) {
		this(control, panel, true);
	}

	/**
	 * ���������캯��
	 * 
	 * @param control  �༭�ؼ�
	 * @param panel    �������
	 * @param editable �Ƿ���Ա༭
	 */
	public CellSelectListener(SplControl control, ContentPanel panel, boolean editable) {
		this.control = control;
		this.cp = panel;
		this.editable = editable;
	}

	/**
	 * �������¼�
	 */
	public void mouseEntered(MouseEvent e) {
	}

	/**
	 * ��갴��ʱ�Ĵ����������λ�õĵ�Ԫ����Ϊ��ǰ��
	 * 
	 * @param e ����¼�
	 */
	public void mousePressed(MouseEvent e) {
		if (dragTimer != null) {
			dragTimer.stop();
			dragTimer = null;
		}
		if (!editable) {
			showPopup(e);
			return;
		}
		CellLocation pos = ControlUtils.lookupCellPosition(e.getX(), e.getY(), cp);
		if (pos == null) {
			return;
		}
		if (control.isSelectingCell()) {
			control.setActiveCell(pos);
			return;
		}
		int row = pos.getRow();
		int col = pos.getCol();
		boolean isActiveCell = false;
		if (control.getActiveCell() != null) {
			if (control.getActiveCell().getRow() == row && control.getActiveCell().getCol() == col) {
				isActiveCell = true;
			}
		}

		boolean isCellSelected = false;
		for (int i = 0; i < control.getSelectedAreas().size(); i++) {
			Area a = control.getSelectedArea(i);
			if (a == null) {
				continue;
			}
			if (a.contains(pos.getRow(), pos.getCol())) {
				isCellSelected = true;
				break;
			}
		}

		if (e.getButton() == MouseEvent.BUTTON1 || !isCellSelected) {
			Area a = null;
			if (!control.getSelectedAreas().isEmpty()) {
				a = control.getSelectedArea(-1);
			}
			if (!e.isControlDown()) {
				control.clearSelectedArea();
			}
			if (e.isShiftDown() && a != null) {
				int startRow = a.getBeginRow();
				int endRow = a.getEndRow();
				int startCol = a.getBeginCol();
				int endCol = a.getEndCol();
				if (pos.getRow() <= startRow) {
					startRow = pos.getRow();
				} else {
					endRow = pos.getRow();
				}
				if (pos.getCol() <= startCol) {
					startCol = pos.getCol();
				} else {
					endCol = pos.getCol();
				}
				a = new Area(startRow, startCol, endRow, endCol);
				control.addSelectedArea(a, true);
				ControlUtils.scrollToVisible(control.getViewport(), control, pos.getRow(), pos.getCol());
				control.repaint();
			} else {
				cp.rememberedRow = pos.getRow();
				cp.rememberedCol = pos.getCol();
				a = new Area(pos.getRow(), pos.getCol(), pos.getRow(), pos.getCol());
				a = control.setActiveCell(pos);
				control.addSelectedArea(a, false);
				control.repaint();
				cp.requestFocus();
			}
		}

		if (isActiveCell && e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() != 2) {
			cp.initEditor(ContentPanel.MODE_SHOW);
			cp.editorMousePressed(e, row, col);
		}

		final MouseEvent me = e;
		Thread t = new Thread() {
			public void run() {
				showPopup(me);
			}
		};
		SwingUtilities.invokeLater(t); // �ӳٵ���������˵��ڿؼ��б���ס
	}

	/**
	 * ����ͷ�ʱ�Ĵ�������༭�����͵�Ԫ��ѡ���¼�
	 * 
	 * @param e ����¼�
	 */
	public void mouseReleased(MouseEvent e) {
		if (dragTimer != null) {
			dragTimer.stop();
			dragTimer = null;
		}
		if (!editable) {
			showPopup(e);
			return;
		}
		if (control.isSelectingCell()) {
			return;
		}
		CellLocation pos = ControlUtils.lookupCellPosition(e.getX(), e.getY(), cp);
		if (pos == null) {
			return;
		}
		int row = pos.getRow();
		int col = pos.getCol();
		setCursor(control.cellSet.getCell(row, col));

		control.fireRegionSelect(false); // ����2��ˢ��
		control.status = GCSpl.STATUS_NORMAL;

		final MouseEvent me = e;
		Thread t = new Thread() {
			public void run() {
				showPopup(me);
			}
		};
		SwingUtilities.invokeLater(t); // �ӳٵ���������˵��ڿؼ��б���ס
	}

	/**
	 * �����˫��ʱ�Ĵ����������˫���ĵ�Ԫ����ͳ��ͼ���򵯳�ͳ��ͼ���Ա༭����
	 * 
	 * @param e ����¼�
	 */
	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
			control.fireDoubleClicked(e);
		}

	}

	/**
	 * ����˳��¼�
	 */
	public void mouseExited(MouseEvent e) {
	}

	/**
	 * ��ס�������ƶ����ʱ�Ĵ��������ݰ���ʱ��λ�ú͵�ǰ�ƶ���λ�ã�����Ԫ��ѡ���
	 * 
	 * @param e ����¼�
	 */
	public void mouseDragged(MouseEvent e) {
		if (dragTimer != null) {
			dragTimer.stop();
			dragTimer = null;
		}
		if (!editable) {
			return;
		}
		whenCursorMoving(e);
	}

	/**
	 * ����ƶ��¼�
	 */
	public void mouseMoved(MouseEvent e) {
		CellLocation pos = ControlUtils.lookupCellPosition(e.getX(), e.getY(), cp);
		if (pos == null) {
			cp.setToolTipText(null);
			return;
		}
		int row = pos.getRow();
		int col = pos.getCol();
		if (row <= control.cellSet.getRowCount() && col <= control.cellSet.getColCount()) {
			String tips = ((PgmNormalCell) control.cellSet.getCell(row, col)).getTip();
			tips = GM.transTips(tips);
			if (StringUtils.isValidString(tips)) {
				cp.setToolTipText(tips);
			} else {
				cp.setToolTipText(null);
			}
			control.fireMouseMove(row, col);
			setCursor(control.cellSet.getCell(row, col));
		}
	}

	/**
	 * ����ƶ�
	 * 
	 * @param e
	 */
	private void whenCursorMoving(MouseEvent e) {
		if (control.getActiveCell() == null) {
			return;
		}
		int x = e.getX(), y = e.getY();
		CellLocation pos = ControlUtils.lookupCellPosition(x, y, cp);
		if (pos == null) {
			if (e.isControlDown() || e.isShiftDown()) {
				return;
			}
			holdDrag(x, y);
		} else {
			int row = pos.getRow();
			int col = pos.getCol();
			if (row == 0 || col == 0) {
				return;
			}
			scrollToArea(row, col);
		}
	}

	/**
	 * ��ʼ��ק
	 * 
	 * @param x
	 * @param y
	 */
	private void holdDrag(int x, int y) {
		row = 0;
		col = 0;
		forward = 0;
		CellSetParser parser = new CellSetParser(control.cellSet);
		if (y < cp.cellY[cp.drawStartRow][cp.drawStartCol]) { // up
			row = cp.drawStartRow;
			for (int r = cp.drawStartRow - 1; r >= 1; r--) { // ��һ����������
				if (parser.isRowVisible(r)) {
					row = r;
					break;
				}
			}
			forward += FORWARD_UP;
		}
		if (y > cp.cellY[cp.drawEndRow][cp.drawStartCol] + cp.cellH[cp.drawEndRow][cp.drawStartCol]) { // down
			int count = control.cellSet.getRowCount();
			row = cp.drawEndRow;
			for (int r = cp.drawEndRow + 1; r <= count; r++) { // ��һ����������
				if (parser.isRowVisible(r)) {
					row = r;
					break;
				}
			}
			forward += FORWARD_DOWN;
		}
		if (x < cp.cellX[cp.drawStartRow][cp.drawStartCol]) { // left
			col = cp.drawStartCol;
			for (int c = cp.drawStartCol - 1; c >= 1; c--) { // ��һ����������
				if (parser.isColVisible(c)) {
					col = c;
					break;
				}
			}
			forward += FORWARD_LEFT;
		}
		if (x > cp.cellX[cp.drawStartRow][cp.drawEndCol] + cp.cellW[cp.drawStartRow][cp.drawEndCol]) { // right
			int count = control.cellSet.getColCount();
			col = cp.drawEndCol;
			for (int c = cp.drawEndCol + 1; c <= count; c++) { // ��һ����������
				if (parser.isColVisible(c)) {
					col = c;
					break;
				}
			}
			forward += FORWARD_RIGHT;
		}
		if (row == 0 || row > control.cellSet.getRowCount()) {
			for (int i = 1; i < cp.cellX.length; i++) {
				for (int j = 1; j < cp.cellX[i].length; j++) {
					if (y > cp.cellY[i][j] && y <= cp.cellY[i][j] + cp.cellH[i][j]) {
						row = i;
						break;
					}
				}
			}
		}
		if (col == 0 || col > control.cellSet.getColCount()) {
			for (int i = 1; i < cp.cellX.length; i++) {
				for (int j = 1; j < cp.cellX[i].length; j++) {
					if (x > cp.cellX[i][j] && x <= cp.cellX[i][j] + cp.cellW[i][j]) {
						col = j;
						break;
					}
				}
			}
		}
		if (row == 0 || col == 0) {
			return;
		}
		dragTimer = new javax.swing.Timer(200, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (row <= 0 || col <= 0 || row > control.cellSet.getRowCount()
						|| col > control.cellSet.getColCount()) {
					return;
				}
				scrollToArea(row, col);
				if ((forward & FORWARD_UP) == FORWARD_UP) {
					row--;
				}
				if ((forward & FORWARD_DOWN) == FORWARD_DOWN) {
					row++;
				}
				if ((forward & FORWARD_LEFT) == FORWARD_LEFT) {
					col--;
				}
				if ((forward & FORWARD_RIGHT) == FORWARD_RIGHT) {
					col++;
				}
				int delay = dragTimer.getDelay();
				if (delay > 10) {
					delay -= 30;
					if (delay < 5) {
						delay = 5;
					}
					dragTimer.setDelay(delay);
					dragTimer.restart();
				}
			}
		});
		dragTimer.setInitialDelay(5);
		dragTimer.start();
	}

	/**
	 * ������ָ��λ��
	 * 
	 * @param row �к�
	 * @param col �к�
	 */
	private void scrollToArea(int row, int col) {
		control.status = GC.STATUS_SELECTING;
		Area area = new Area(control.getActiveCell().getRow(), control.getActiveCell().getCol(), row, col);
		area = ControlUtils.adjustArea(area);
		ControlUtils.scrollToVisible(control.getViewport(), control, row, col);
		control.addSelectedArea(area, true);
		control.repaint();
		SplEditor editor = ControlUtils.extractSplEditor(control);
		if (editor != null) {
			editor.setSelectedAreas(control.getSelectedAreas());
			editor.resetSelectedAreas();
		}
	}

	/**
	 * �����뿪
	 */
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
			isCtrlDown = false;
		}
	}

	/**
	 * ��ѡ��Ԫ��δ��ý���ʱ�����̱����µĴ��� �����µ��Ƿ��������Ӧ�ı䵱ǰ���Ԫ��
	 * 
	 * @param e �����¼�
	 */
	public void keyPressed(KeyEvent e) {
		if (!editable) {
			return;
		}
		CellLocation activeCell = control.getActiveCell();
		if (activeCell == null) {
			return;
		}
		int curCol = activeCell.getCol();
		int curRow = activeCell.getRow();
		Vector<IAtomicCmd> cmds = new Vector<IAtomicCmd>();
		CellSet ics = control.getCellSet();

		CellRect srcRect, tarRect;
		SplEditor editor = ControlUtils.extractSplEditor(control);
		if (editor == null) {
			return;
		}

		int key = e.getKeyCode();
		switch (key) {
		case KeyEvent.VK_ENTER:
			if (e.isAltDown()) {
				return;
			} else if (e.isShiftDown()) {
				return;
			} else if (e.isControlDown()) {
				editor.hotKeyInsert(SplEditor.HK_CTRL_ENTER);
			} else if (GM.isMacOS() && e.isMetaDown()) {
				editor.hotKeyInsert(SplEditor.HK_CTRL_ENTER);
			} else {
				GVSpl.panelValue.tableValue.setLocked(false);
				CellSetParser parser = new CellSetParser(control.cellSet);
				PgmNormalCell cell;
				int nextCol = -1;
				for (int c = curCol + 1; c <= control.cellSet.getColCount(); c++) {
					if (parser.isColVisible(c)) {
						cell = control.cellSet.getPgmNormalCell(curRow, c);
						if (!cell.isNoteBlock() && !cell.isNoteCell()) {
							if (StringUtils.isValidString(parser.getDispText(curRow, c))) {
								nextCol = c;
								break;
							}
						}
					}
				}
				if (nextCol > 0) {
					control.scrollToArea(control.setActiveCell(new CellLocation(curRow, nextCol)));
				} else {
					if (curRow == ics.getRowCount()) {
						editor.appendRows(1);
					}
					control.scrollToArea(control.toDownCell());
				}
			}
			break;
		case KeyEvent.VK_BACK_SPACE:
			if (GM.isMacOS()) {
				if (!e.isAltDown() && !e.isMetaDown() && !e.isShiftDown()) {
					editor.clear(SplEditor.CLEAR_EXP);
					break;
				}
				return;
			} else {
				if (e.isControlDown()) {
					if (curCol > 1) {
						int moveCols = (int) (ics.getColCount() - curCol + 1);
						srcRect = new CellRect(curRow, curCol, 1, moveCols);
						tarRect = new CellRect(curRow, (int) (curCol - 1), 1, moveCols);
						moveRect(srcRect, tarRect);
					} else if (curRow > 1) {
						int topUsedCols = getUsedCols(curRow - 1);
						connectRowUpTo(curRow, (int) (topUsedCols + 1));
					}
				} else {
					AtomicCell ac = new AtomicCell(control, ics.getCell(activeCell.getRow(), activeCell.getCol()));
					ac.setProperty(AtomicCell.CELL_EXP);
					ac.setValue(null);
					cmds.add(ac);
					ControlUtils.extractSplEditor(control).executeCmd(cmds);
				}
			}
			break;
		case KeyEvent.VK_F2:
			int ca = 0;
			try {
				CellLocation cl = control.getActiveCell();
				String text = ControlUtils.getCellText(control.cellSet, cl.getRow(), cl.getCol(), true);
				ca = text.length();
			} catch (Throwable t) {
			}
			cp.initEditor(ca, ContentPanel.MODE_SHOW);
			break;
		case KeyEvent.VK_HOME:
			if (e.isControlDown()) {
				int firstVisibleRow = 1;
				int rowCount = control.cellSet.getRowCount();
				CellSetParser parser = new CellSetParser(ics);
				while (!parser.isRowVisible(firstVisibleRow)) {
					firstVisibleRow++;
					if (firstVisibleRow > rowCount)
						return;
				}
				if (e.isShiftDown()) {
					Area area = control.getSelectedArea(0);
					Area newArea = new Area(firstVisibleRow, area.getBeginCol(), activeCell.getRow(), area.getEndCol());
					control.selectToArea(newArea);
				} else {
					control.scrollToArea(control.setActiveCell(new CellLocation(firstVisibleRow, curCol)));
				}
			} else {
				int firstCol = getFirstNonEmptyColumn(curRow);
				if (curCol == firstCol) {
					firstCol = 1;
				}
				control.scrollToArea(control.setActiveCell(new CellLocation(curRow, firstCol)));
			}
			break;
		case KeyEvent.VK_END:
			if (e.isControlDown()) {
				int lastVisibleRow = control.cellSet.getRowCount();
				CellSetParser parser = new CellSetParser(ics);
				while (!parser.isRowVisible(lastVisibleRow)) {
					lastVisibleRow--;
					if (lastVisibleRow < 1)
						return;
				}
				if (e.isShiftDown()) {
					Area area = control.getSelectedArea(0);
					Area newArea = new Area(activeCell.getRow(), area.getBeginCol(), lastVisibleRow, area.getEndCol());
					control.selectToArea(newArea);
				} else {
					control.scrollToArea(control.setActiveCell(new CellLocation(lastVisibleRow, curCol)));
				}
			} else {
				control.scrollToArea(control.setActiveCell(new CellLocation(curRow, (int) ics.getColCount())));
			}
			break;
		case KeyEvent.VK_PAGE_UP: {
			final int PAGE_WIDTH = GM.getPageWidth(control.scale);
			final int PAGE_HEIGHT = GM.getPageHeight(control.scale);
			CellSetParser parser = new CellSetParser(control.cellSet);
			if (e.isControlDown()) {
				int preVisibleCol = curCol;
				int tmpWidth = parser.getColWidth(curCol, control.scale);
				for (int c = curCol - 1; c >= 1; c--) {
					if (!parser.isColVisible(c))
						continue;
					preVisibleCol = c;
					tmpWidth += parser.getColWidth(curCol, control.scale);
					if (tmpWidth > PAGE_WIDTH) {
						break;
					}
				}
				if (e.isShiftDown()) {
					Area area = control.getSelectedArea(0);
					Area newArea = new Area(area.getBeginRow(), preVisibleCol, area.getEndRow(), activeCell.getCol());
					control.selectToArea(newArea);
				} else {
					control.scrollToArea(control.setActiveCell(new CellLocation(curRow, preVisibleCol)));
				}
			} else {
				int tmpHeight = parser.getRowHeight(curRow, control.scale);
				int preVisibleRow = curRow;
				for (int r = curRow - 1; r >= 1; r--) {
					if (!parser.isRowVisible(r))
						continue;
					preVisibleRow = r;
					tmpHeight += parser.getRowHeight(curRow, control.scale);
					if (tmpHeight > PAGE_HEIGHT) {
						break;
					}
				}
				control.scrollToArea(control.setActiveCell(new CellLocation(preVisibleRow, curCol)));
				if (preVisibleRow == 1) {
					control.getVerticalScrollBar().setValue(0);
				}
			}
		}
			break;
		case KeyEvent.VK_PAGE_DOWN: {
			CellSetParser parser = new CellSetParser(control.cellSet);
			final int PAGE_WIDTH = GM.getPageWidth(control.scale);
			final int PAGE_HEIGHT = GM.getPageHeight(control.scale);
			if (e.isControlDown()) {
				int deltaW = cp.getColOffset(curCol) - control.getHorizontalScrollBar().getValue();
				int subVisibleCol = curCol;
				int colCount = control.cellSet.getColCount();
				int tmpWidth = parser.getColWidth(curCol, control.scale);
				for (int c = curCol + 1; c <= colCount; c++) {
					if (!parser.isColVisible(c))
						continue;
					subVisibleCol = c;
					tmpWidth += parser.getColWidth(curCol, control.scale);
					if (tmpWidth > PAGE_WIDTH) {
						break;
					}
				}
				if (e.isShiftDown()) {
					Area area = control.getSelectedArea(0);
					Area newArea = new Area(area.getBeginRow(), activeCell.getCol(), area.getEndRow(), subVisibleCol);
					control.selectToArea(newArea);
				} else {
					control.scrollToArea(control.setActiveCell(new CellLocation(curRow, subVisibleCol)));
				}
				control.getHorizontalScrollBar().setValue(cp.getColOffset(subVisibleCol) - deltaW);
			} else {
				int tmpHeight = parser.getRowHeight(curRow, control.scale);
				int subVisibleRow = curRow;
				int rowCount = control.cellSet.getRowCount();
				for (int r = curRow + 1; r <= rowCount; r++) {
					if (!parser.isRowVisible(r))
						continue;
					subVisibleRow = r;
					tmpHeight += parser.getRowHeight(curRow, control.scale);
					if (tmpHeight > PAGE_HEIGHT) {
						break;
					}
				}
				control.scrollToArea(control.setActiveCell(new CellLocation(subVisibleRow, curCol)));
			}
		}
			break;
		case KeyEvent.VK_UP: // ��
			if (e.isShiftDown()) {
				CellLocation tarPos = null;
				if (e.isControlDown()) {
					Area area = control.getSelectedArea(0);
					CellLocation pos = new CellLocation(activeCell.getRow(), activeCell.getCol());
					if (area.getEndRow() == activeCell.getRow()) {
						pos.setRow(area.getBeginRow());
					}
					tarPos = getNextPos(pos, NEXT_TOP);
				}
				control.selectToUpCell(tarPos);
			} else if (e.isAltDown()) {
				return;
			} else if (e.isMetaDown()) {
				return;
			} else if (e.isControlDown()) {
				control.scrollToArea(control.setActiveCell(getNextPos(activeCell, NEXT_TOP)));
			} else {
				control.scrollToArea(control.toUpCell());
			}
			break;
		case KeyEvent.VK_DOWN: // ��
			if (e.isShiftDown()) {
				CellLocation tarPos = null;
				if (e.isControlDown()) {
					Area area = control.getSelectedArea(0);
					CellLocation pos = new CellLocation(activeCell.getRow(), activeCell.getCol());
					if (area.getBeginRow() == activeCell.getRow()) {
						pos.setRow(area.getEndRow());
					}
					tarPos = getNextPos(pos, NEXT_BOTTOM);
				}
				control.selectToDownCell(tarPos);
			} else if (e.isAltDown()) {
				return;
			} else if (e.isMetaDown()) {
				return;
			} else if (e.isControlDown()) {
				control.scrollToArea(control.setActiveCell(getNextPos(activeCell, NEXT_BOTTOM)));
			} else {
				control.scrollToArea(control.toDownCell());
			}
			break;
		case KeyEvent.VK_LEFT: // ��
			if (e.isShiftDown()) {
				CellLocation tarPos = null;
				if (e.isControlDown()) {
					Area area = control.getSelectedArea(0);
					CellLocation pos = new CellLocation(activeCell.getRow(), activeCell.getCol());
					if (area.getEndCol() == activeCell.getCol()) {
						pos.setRow(area.getBeginCol());
					}
					tarPos = getNextPos(pos, NEXT_LEFT);
				}
				control.selectToLeftCell(tarPos);
			} else if (e.isAltDown()) {
				return;
			} else if (e.isMetaDown()) {
				return;
			} else if (e.isControlDown()) {
				control.scrollToArea(control.setActiveCell(getNextPos(activeCell, NEXT_LEFT)));
			} else {
				control.scrollToArea(control.toLeftCell());
			}
			break;
		case KeyEvent.VK_RIGHT: // ��
			if (e.isShiftDown()) {
				CellLocation tarPos = null;
				if (e.isControlDown()) {
					Area area = control.getSelectedArea(0);
					CellLocation pos = new CellLocation(activeCell.getRow(), activeCell.getCol());
					if (area.getBeginCol() == activeCell.getCol()) {
						pos.setCol(area.getEndCol());
					}
					tarPos = getNextPos(pos, NEXT_RIGHT);
				}
				control.selectToRightCell(tarPos);
			} else if (e.isAltDown()) {
				return;
			} else if (e.isMetaDown()) {
				return;
			} else if (e.isControlDown()) {
				control.scrollToArea(control.setActiveCell(getNextPos(activeCell, NEXT_RIGHT)));
			} else {
				control.scrollToArea(control.toRightCell());
			}
			break;
		case KeyEvent.VK_DELETE:
			if (GM.isMacOS()) {
				AtomicCell ac = new AtomicCell(control, ics.getCell(activeCell.getRow(), activeCell.getCol()));
				ac.setProperty(AtomicCell.CELL_EXP);
				ac.setValue(null);
				cmds.add(ac);
				ControlUtils.extractSplEditor(control).executeCmd(cmds);
				break;
			}
			return;
		case KeyEvent.VK_INSERT:
			if (e.isControlDown()) {
				editor.hotKeyInsert(SplEditor.HK_CTRL_INSERT);
			} else if (e.isAltDown()) {
				editor.hotKeyInsert(SplEditor.HK_ALT_INSERT);
			}
			break;
		case KeyEvent.VK_TAB:
			if (e.isShiftDown()) {
				control.scrollToArea(control.toLeftCell());
			} else if (e.isControlDown()) {
				// CTRL-TAB���ͳ��л���ǰ�SHEET������EXCEL��
				((SPL) GVSpl.appFrame).showNextSheet(isCtrlDown);
				isCtrlDown = true;
			} else {
				if (curCol == ics.getColCount()) {
					editor.appendCols(1);
				}
				control.scrollToArea(control.toRightCell());
			}
			break;
		case KeyEvent.VK_ESCAPE:
			// �ڵ�ǰ���ڲ����ѡ���
			control.resetCellSelection(null);
			break;
		case KeyEvent.VK_I:
			if (GM.isMacOS()) {
				if (e.isMetaDown() && !e.isControlDown() && !e.isAltDown()) {
					editor.hotKeyInsert(SplEditor.HK_CTRL_INSERT);
				}
			} else {
				if (e.isControlDown() && !e.isShiftDown() && !e.isAltDown()) {
					if (GV.appSheet != null) {
						((SheetSpl) GV.appSheet).importSameNameTxt();
					}
				}
			}
			break;
		default:
			if (e.getKeyCode() == KeyEvent.VK_A && e.isControlDown() && !e.isShiftDown() && !e.isAltDown()) {
				control.selectAll();
				break;
			}
			if (e.getKeyCode() == KeyEvent.VK_W && e.isControlDown() && !e.isShiftDown() && !e.isAltDown()) {
				if (GV.appSheet != null)
					GV.appFrame.closeSheet(GV.appSheet);
				break;
			}
			if (e.isActionKey()) {
				return;
			}
			if (e.isControlDown() || e.isAltDown() || e.getKeyCode() == KeyEvent.VK_SHIFT || e.isMetaDown()) {
				return;
			}
			if (cp.getEditor() == null || !cp.getEditor().isVisible()) {
				cp.initEditor(ContentPanel.MODE_SHOW);
			}
			return;
		}
		e.consume();
	}

	/**
	 * �Ƿ�����CTRL��
	 */
	private boolean isCtrlDown = false;

	/**
	 * ���ù��
	 * 
	 * @param cell
	 */
	private void setCursor(INormalCell cell) {
		boolean isActive = false;
		if (control.getActiveCell() != null) {
			if (control.getActiveCell().getRow() == cell.getRow()
					&& control.getActiveCell().getCol() == cell.getCol()) {
				isActive = true;
			}
		}
		if (isActive) {
			cp.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		} else {
			cp.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}

	/**
	 * ��connnectRow���ӵ���һ�е�upColλ��
	 * 
	 * @param connectRow int
	 * @param upCol      int
	 */
	private void connectRowUpTo(int connectRow, int upCol) {
		int usedCols = getUsedCols(connectRow);
		if (usedCols == 0) { // Ctrl-BackSpace����ҵ�����
			usedCols = 1;
		}
		CellRect srcRect = new CellRect(connectRow, (int) 1, 1, usedCols);
		CellRect tarRect = new CellRect(connectRow - 1, upCol, 1, usedCols);
		Vector<IAtomicCmd> cmds = GMSpl.getMoveRectCmd(ControlUtils.extractSplEditor(control), srcRect, tarRect);
		if (cmds != null && !cmds.isEmpty()) {
			AtomicSpl cmd = new AtomicSpl(control);
			cmd.setType(AtomicSpl.REMOVE_ROW);
			CellRect rect = new CellRect(connectRow, (int) 1, 1, (int) control.cellSet.getColCount());
			cmd.setRect(rect);
			cmds.add(cmd);
			ControlUtils.extractSplEditor(control).executeCmd(cmds);
			control.scrollToArea(control.setActiveCell(new CellLocation(tarRect.getBeginRow(), tarRect.getBeginCol())));
		}
	}

	/**
	 * �ҵ���ǰ�еĵ�һ�в�Ϊ�յĸ��ӵ��кţ� ����Home���л�
	 * 
	 * @param row int
	 * @return int
	 */
	private int getFirstNonEmptyColumn(int row) {
		CellSet ics = control.getCellSet();
		for (int c = 1; c <= ics.getColCount(); c++) {
			NormalCell nc = (NormalCell) ics.getCell(row, c);
			if (StringUtils.isValidString(nc.getExpString())) {
				return c;
			}
		}
		return 1; // ȫ��Ϊ���򷵻ص�1��
	}

	/**
	 * �ҵ�ǰ�������һ�������ȵĸ������� ����Ctrl+����ʱ�ı༭
	 * 
	 * @param pos       CellPosition
	 * @param direction byte
	 * @return CellPosition
	 */
	private CellLocation getNextPos(CellLocation pos, byte direction) {
		CellSet ics = control.getCellSet();
		CellSetParser parser = new CellSetParser(ics);
		int row = pos.getRow();
		int col = pos.getCol();
		int newr = row;
		int newc = col;
		NormalCell nc;
		// �ҵ���һ���д��ĸ���
		switch (direction) {
		case NEXT_TOP:
			newr = 1;
			for (int r = row - 1; r >= 1; r--) {
				if (!parser.isRowVisible(r))
					continue;
				nc = (NormalCell) ics.getCell(r, col);
				if (StringUtils.isValidString(nc.getExpString())) {
					newr = r;
					break;
				}
			}
			break;
		case NEXT_BOTTOM:
			newr = ics.getRowCount();
			for (int r = row + 1; r <= ics.getRowCount(); r++) {
				if (!parser.isRowVisible(r))
					continue;
				nc = (NormalCell) ics.getCell(r, col);
				if (StringUtils.isValidString(nc.getExpString())) {
					newr = r;
					break;
				}
			}
			break;
		case NEXT_LEFT:
			newc = 1;
			for (int c = (int) (col - 1); c >= 1; c--) {
				if (!parser.isColVisible(c))
					continue;
				nc = (NormalCell) ics.getCell(row, c);
				if (StringUtils.isValidString(nc.getExpString())) {
					newc = c;
					break;
				}
			}
			break;
		case NEXT_RIGHT:
			newc = (int) ics.getColCount();
			for (int c = (int) (col + 1); c <= ics.getColCount(); c++) {
				if (!parser.isColVisible(c))
					continue;
				nc = (NormalCell) ics.getCell(row, c);
				if (StringUtils.isValidString(nc.getExpString())) {
					newc = c;
					break;
				}
			}
			break;
		}

		return new CellLocation(newr, newc);
	}

	/**
	 * ��ȡ��ǰ����ռ�õ����� ����Ctrl-BackSpace
	 * 
	 * @param row int
	 * @return int
	 */
	public int getUsedCols(int row) {
		CellSet ics = control.getCellSet();
		int colCount = (int) ics.getColCount();
		return (int) (colCount - getEmptyColumns(row));
	}

	/**
	 * �õ���β�Ŀհ׸��ӵ����� ����Ctrl-BackSpace
	 * 
	 * @param row int
	 * @return int
	 */
	private int getEmptyColumns(int row) {
		CellSet ics = control.getCellSet();
		int colCount = (int) ics.getColCount();
		for (int c = colCount; c >= 1; c--) {
			NormalCell nc = (NormalCell) ics.getCell(row, c);
			if (StringUtils.isValidString(nc.getExpString())) {
				return (int) (colCount - c);
			}
		}
		return colCount;
	}

	/**
	 * ȡ����ʹ�õ��������ǿհ��У�
	 * 
	 * @param col
	 * @return
	 */
	public int getUsedRows(int col) {
		CellSet ics = control.getCellSet();
		int rowCount = ics.getRowCount();
		return rowCount - getEmptyRows(col);
	}

	/**
	 * ȡ���пհ�����
	 * 
	 * @param col
	 * @return
	 */
	private int getEmptyRows(int col) {
		CellSet ics = control.getCellSet();
		int rowCount = ics.getRowCount();
		for (int r = rowCount; r >= 1; r--) {
			NormalCell nc = (NormalCell) ics.getCell(r, col);
			if (StringUtils.isValidString(nc.getExpString())) {
				return rowCount - r;
			}
		}
		return rowCount;
	}

	/**
	 * �ƶ���������
	 * 
	 * @param srcRect Դ��������
	 * @param tarRect Ŀ���������
	 * @return
	 */
	private boolean moveRect(CellRect srcRect, CellRect tarRect) {
		return moveRect(srcRect, tarRect, true);
	}

	/**
	 * �ƶ���������
	 * 
	 * @param srcRect        Դ��������
	 * @param tarRect        Ŀ���������
	 * @param scrollToTarget �Ƿ������Ŀ������
	 * @return
	 */
	public boolean moveRect(CellRect srcRect, CellRect tarRect, boolean scrollToTarget) {
		Vector<IAtomicCmd> cmds = GMSpl.getMoveRectCmd(ControlUtils.extractSplEditor(control), srcRect, tarRect);
		if (cmds == null) {
			return false;
		}
		ControlUtils.extractSplEditor(control).executeCmd(cmds);
		if (scrollToTarget) {
			control.scrollToArea(control.setActiveCell(new CellLocation(tarRect.getBeginRow(), tarRect.getBeginCol())));
		}
		return true;
	}

	/**
	 * ��ѡ��Ԫ��δ��ý���ʱ�����̱�����Ĵ��� ���û�м������뷨���򽫱�������ַ���Ϊ��ǰ��Ԫ���ֵ����ʹ��ǰ��Ԫ���ý���
	 * 
	 * @param e �����¼�
	 */
	public void keyTyped(KeyEvent e) {
		if (!editable) {
			return;
		}
		char c = e.getKeyChar();
		if (!Character.isDefined(c)) {
			return;
		}
		if (e.isControlDown()) {
			return;
		}
		if (e.isAltDown()) {
			return;
		}
		if (c == '\u001B' || c == '\n' || c == '\b' || c == '\t') {
			return;
		}
		if (cp.getEditor() == null) {
			return;
		}
		if (cp.getEditor() instanceof JTextComponent) {
			cp.getEditor().requestFocus();
			String text = String.valueOf(e.getKeyChar());
			((JTextComponent) cp.getEditor()).setText(text);
		}

	}

	/**
	 * �Ҽ������˵�
	 * 
	 * @param e
	 */
	void showPopup(MouseEvent e) {
		if (e.isPopupTrigger()) {
			control.fireRightClicked(e, GC.SELECT_STATE_CELL);
		}
	}

}