<div class="wb_comments">
	<br /><hr /><br />
	<a name="wb_comment_box"></a>
	<div class="wb_comments_form">
		<form method="post">
			<input type="hidden" name="postComment" value="1" />
			<textarea name="message" class="hpc"></textarea>
			<table>
				<tr>
					<td>
						<input class="span4" type="text" name="name" placeholder="Name" />
					</td>
					<td style="text-align: right; vertical-align: top;">
						<button type="submit" class="btn">Send</button>
					</td>
				</tr>
				<tr>
					<td colspan="2">
						<textarea class="span5" name="text" cols="40" rows="3" placeholder="Message"></textarea>
					</td>
				</tr>
			</table>
		</form>
	</div><?php
foreach ($comments as $li) { ?>

	<div class="wb_comment">
		<div class="wb_comment_user">
			<?php echo $li->user; ?>
			<span class="wb_comment_date"><?php echo $li->date.' '.$li->time; ?></span>
		</div>
		<div class="wb_comment_text"><?php echo $li->text; ?></div>
	</div><?php
}
if (empty($comments)) { ?>

	<div>No comments, be the firt one to comment.</div><?php
} ?>

</div>