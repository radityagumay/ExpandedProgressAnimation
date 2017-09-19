package radityalabs.expandedprogressanimation;

import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout container;
    private TextView textview;

    private boolean isExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        textview = (TextView) findViewById(R.id.textview);
        container = (RelativeLayout) findViewById(R.id.container);

        GradientDrawable iconBackground = new GradientDrawable();
        iconBackground.setCornerRadius(100);
        iconBackground.setColor(getResources().getColor(R.color.colorAccent));
        container.setBackground(iconBackground);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            container.setElevation(4);
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isExpanded) {
                    isExpanded = true;
                    ViewAnimator.animate(container)
                            .dp()
                            .width(60f, 250f)
                            .interpolator(new DecelerateInterpolator())
                            .duration(800)
                            .onStop(new AnimationListener.Stop() {
                                @Override
                                public void onStop() {
                                    textview.setVisibility(View.VISIBLE);
                                    textview.setText("Jakarta Pusat");
                                    ViewAnimator.animate(textview)
                                            .dp().translationY(50, 0)
                                            .alpha(0.1f, 1)
                                            .singleInterpolator(new OvershootInterpolator())
                                            .duration(800)
                                            .start();
                                }
                            })
                            .start();
                    ViewAnimator
                            .animate(container).scaleY(0, 1).decelerate().duration(500);
                } else {
                    isExpanded = false;
                    ViewAnimator.animate(textview)
                            .dp().translationY(0, 50)
                            .alpha(1, 0.1f)
                            .singleInterpolator(new OvershootInterpolator())
                            .duration(800)
                            .onStart(new AnimationListener.Start() {
                                @Override
                                public void onStart() {
                                    ViewAnimator.animate(container)
                                            .dp()
                                            .width(250f, 60f)
                                            .interpolator(new AccelerateInterpolator())
                                            .duration(1200)
                                            .start();
                                    ViewAnimator
                                            .animate(container).scaleY(1, 0).accelerate().duration(800);

                                }
                            })
                            .onStop(new AnimationListener.Stop() {
                                @Override
                                public void onStop() {
                                    textview.setVisibility(View.GONE);
                                }
                            })
                            .start();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
