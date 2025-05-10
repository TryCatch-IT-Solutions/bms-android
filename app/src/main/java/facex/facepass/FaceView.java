package facex.facepass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.example.bms.R;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by wangjingyi on 28/03/2017.
 */

public class FaceView extends View {
    private List<String> ids;
    private List<String> yaws;
    private List<String> pitchs;
    private List<String> rolls;
    private List<String> blurs;
    private List<String> smiles;
    private List<Rect> rect;
    private Paint paint = new Paint();
    private Paint idPaint = new Paint();
    private Paint posePaint = new Paint();

    int r = 255,g = 255,b = 255;
    private void setFaceRectColor(int r,int g,int b){
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public void setFaceRectNormal(){
        this.r = 255;
        this.g = 255;
        this.b = 255;
    }

    public void setFaceRectUser(){
        this.r = 20;
        this.g = 233;
        this.b = 20;
    }

    public void setFaceRectError(){
        this.r = 233;
        this.g = 20;
        this.b = 20;
    }

    private void initData() {
        ids = new ArrayList<String>();
        yaws = new ArrayList<>();
        pitchs = new ArrayList<>();
        rolls = new ArrayList<>();
        blurs = new ArrayList<>();
        smiles = new ArrayList<>();
        rect = new ArrayList<Rect>();
        paint.setARGB(122, r, g, b);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10.0f);

        idPaint.setARGB(122, 255, 255, 255);
        idPaint.setTextSize(40);

        posePaint.setARGB(255, 80, 80, 80);
        posePaint.setTextSize(25);
    }

    public FaceView(Context context) {
        super(context);
        initData();
    }

    public FaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initData();
    }

    public FaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initData();
    }

    public void addId(String label) {
        ids.add(label);
    }

    public void addYaw(String label) {
        yaws.add(label);
    }
    public void addPitch(String label) {
        pitchs.add(label);
    }
    public void addRoll(String label) {
        rolls.add(label);
    }
    public void addBlur(String label)  {
        blurs.add(label);
    }
    public void addSmile(String lable) {
        smiles.add(lable);
    }

    public void addRect(RectF rect) {
        Rect buffer = new Rect();
        buffer.left = (int) rect.left;
        buffer.top = (int) rect.top;
        buffer.right = (int) rect.right;
        buffer.bottom = (int) rect.bottom;
        this.rect.add(buffer);
    }

    public void clear() {
        rect.clear();
        ids.clear();
        yaws.clear();
        rolls.clear();
        blurs.clear();
        pitchs.clear();
        smiles.clear();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < rect.size(); i++) {
            Rect rr = rect.get(i);
            paint.setARGB(122, r, g, b);
            idPaint.setARGB(122, r, g, b);
            canvas.drawRect(rr, paint);
            String info = "";
            if(g==20){
                info = getContext().getString(R.string.failed);
            }else if(g==233){
                info = getContext().getString(R.string.success);
            }else if(g==255){
                info = getContext().getString(R.string.identifying);
            }
            canvas.drawText(info, rr.right + 5, rr.top + 30, idPaint);
//            canvas.drawText(yaws.get(i), r.right + 5, r.top + 60, posePaint);
//            canvas.drawText(pitchs.get(i), r.right + 5, r.top + 93, posePaint);
//            canvas.drawText(rolls.get(i), r.right + 5, r.top + 126, posePaint);
//            canvas.drawText(blurs.get(i), r.right + 5, r.top + 159, posePaint);
//            canvas.drawText(smiles.get(i), r.right + 5, r.top + 192, posePaint);
        }
        this.clear();
    }
}
