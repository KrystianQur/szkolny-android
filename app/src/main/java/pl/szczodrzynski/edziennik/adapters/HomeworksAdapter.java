package pl.szczodrzynski.edziennik.adapters;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.datamodels.EventFull;
import pl.szczodrzynski.edziennik.dialogs.EventManualDialog;
import pl.szczodrzynski.edziennik.fragments.HomeFragment;
import pl.szczodrzynski.edziennik.models.Date;

import static pl.szczodrzynski.edziennik.utils.Utils.bs;

public class HomeworksAdapter extends RecyclerView.Adapter<HomeworksAdapter.ViewHolder> {
    private Context context;
    private List<EventFull> homeworkList;

    //getting the context and product list with constructor
    public HomeworksAdapter(Context mCtx, List<EventFull> homeworkList) {
        this.context = mCtx;
        this.homeworkList = homeworkList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflating and returning our view holder
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_homeworks_item, parent, false);
        return new ViewHolder(view);
    }

    public static String dayDiffString(Context context, int dayDiff) {
        if (dayDiff > 0) {
            if (dayDiff == 1) {
                return context.getString(R.string.tomorrow);
            }
            else if (dayDiff == 2) {
                return context.getString(R.string.the_day_after);
            }
            return HomeFragment.plural(context, R.plurals.time_till_days, Math.abs(dayDiff));
        }
        else if (dayDiff < 0) {
            if (dayDiff == -1) {
                return context.getString(R.string.yesterday);
            }
            else if (dayDiff == -2) {
                return context.getString(R.string.the_day_before);
            }
            return context.getString(R.string.ago_format, HomeFragment.plural(context, R.plurals.time_till_days, Math.abs(dayDiff)));
        }
        return context.getString(R.string.today);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        App app = (App) context.getApplicationContext();

        EventFull homework = homeworkList.get(position);

        int diffDays = Date.diffDays(homework.eventDate, Date.getToday());

        holder.homeworksItemHomeworkDate.setText(app.getString(R.string.date_relative_format, homework.eventDate.getFormattedString(), dayDiffString(context, diffDays)));
        holder.homeworksItemTopic.setText(homework.topic);
        holder.homeworksItemSubjectTeacher.setText(context.getString(R.string.homeworks_subject_teacher_format, bs(homework.subjectLongName), bs(homework.teacherFullName)));
        holder.homeworksItemTeamDate.setText(context.getString(R.string.homeworks_team_date_format, bs(homework.teamName), Date.fromMillis(homework.addedDate).getFormattedStringShort()));

        if (!homework.seen) {
            holder.homeworksItemTopic.setBackground(context.getResources().getDrawable(R.drawable.bg_rounded_8dp));
            holder.homeworksItemTopic.getBackground().setColorFilter(new PorterDuffColorFilter(0x692196f3, PorterDuff.Mode.MULTIPLY));
            homework.seen = true;
            AsyncTask.execute(() -> {
                app.db.metadataDao().setSeen(App.profileId, homework, true);
            });
        }
        else {
            holder.homeworksItemTopic.setBackground(null);
        }

        holder.homeworksItemEdit.setVisibility((homework.addedManually ? View.VISIBLE : View.GONE));
        holder.homeworksItemEdit.setOnClickListener(v -> {
            new EventManualDialog(context).show(app, homework, null, null, EventManualDialog.DIALOG_HOMEWORK);
        });

        if (homework.sharedBy == null) {
            holder.homeworksItemSharedBy.setVisibility(View.GONE);
        }
        else if (homework.sharedByName != null) {
            holder.homeworksItemSharedBy.setText(app.getString(R.string.event_shared_by_format, (homework.sharedBy.equals("self") ? app.getString(R.string.event_shared_by_self) : homework.sharedByName)));
        }
    }

    @Override
    public int getItemCount() {
        return homeworkList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        CardView homeworksItemCard;
        TextView homeworksItemTopic;
        TextView homeworksItemHomeworkDate;
        TextView homeworksItemSharedBy;
        TextView homeworksItemSubjectTeacher;
        TextView homeworksItemTeamDate;
        Button homeworksItemEdit;

        ViewHolder(View itemView) {
            super(itemView);
            homeworksItemCard = itemView.findViewById(R.id.homeworksItemCard);
            homeworksItemTopic = itemView.findViewById(R.id.homeworksItemTopic);
            homeworksItemHomeworkDate = itemView.findViewById(R.id.homeworksItemHomeworkDate);
            homeworksItemSharedBy = itemView.findViewById(R.id.homeworksItemSharedBy);
            homeworksItemSubjectTeacher = itemView.findViewById(R.id.homeworksItemSubjectTeacher);
            homeworksItemTeamDate = itemView.findViewById(R.id.homeworksItemTeamDate);
            homeworksItemEdit = itemView.findViewById(R.id.homeworksItemEdit);
        }
    }
}