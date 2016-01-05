package de.gymolching.fsb.halFake;

import de.gymolching.fsb.halApi.ArmFactory;
import de.gymolching.fsb.halApi.ArmInterface;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * @author sschaeffner
 */
public class FakeArmFactory implements ArmFactory, FakeArmImpl.PositionChangeListener {

    //singleton instance of FakeArmFactory
    private static FakeArmFactory instance;

    public static FakeArmFactory getInstance() {
        if (instance == null) instance = new FakeArmFactory();
        return instance;
    }

    private FakeArmImpl[] arms;
    private ArmPaint[] armPaints;
    private JFrame frame;

    private FakeArmFactory() {
        arms = new FakeArmImpl[6];
        armPaints = new ArmPaint[6];
        for (int i = 0; i < 6; i++) {
            arms[i] = new FakeArmImpl(this);
            armPaints[i] = new ArmPaint(arms[i], i);
        }
        frame = new JFrame("FakeHAL");
        frame.setContentPane(new HalPanel());
        frame.setSize(800, 600);
        frame.setVisible(true);
    }

    public ArmInterface provideArm(int nr) throws IOException {
        if (nr > 6 || nr < 0) return null;
        return arms[nr];
    }

    @Override
    public void onPositionChange() {
        frame.repaint();
    }

    private class HalPanel extends JPanel {
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            for (ArmPaint ap : armPaints) ap.paint(g);
        }
    }

    private class ArmPaint {
        private static final int MARGIN_LEFT = 10, MARGIN_TOP = 10;
        private static final int SIZE_X = 60, SIZE_Y = 300;

        private final FakeArmImpl arm;
        private final int nr;

        ArmPaint(FakeArmImpl arm, int nr) {
            this.arm = arm;
            this.nr = nr;
        }

        void paint(Graphics g) {
            g.drawRect(MARGIN_LEFT + ((MARGIN_LEFT + SIZE_X) * nr), MARGIN_TOP, SIZE_X, SIZE_Y);
        }
    }
}
