package lila.msg

import lila.common.config.BaseUrl
import lila.memo.MongoCache
import lila.user.UserRepo

final class EmailReminder(mongoCache: MongoCache.Api, userRepo: UserRepo, api: MsgApi, baseUrl: BaseUrl)(using
    Executor
):

  def apply(userId: UserId) = cache get userId

  private val emailReminderMsg = s"""No email associated with your account

Hello, as you are one of lichess oldest users, no email was required when you registered.

However this makes it easy for you to lose access to your account.
If you forget your password, if your password is leaked from another website, or if we decide that your password is too easy-to-guess to be secure, your account will be lost.

You can visit $baseUrl/account/email to set your personal email address."""

  private val cache = mongoCache[UserId, Boolean](1024, "security:email:reminder", 10 days, _.value):
    loader =>
      _.expireAfterWrite(11 days).buildAsyncFuture:
        loader: userId =>
          userRepo
            .withoutEmail(userId)
            .flatMap:
              case Some(user) =>
                api.systemPost(userId, emailReminderMsg) inject false
              case _ => fuccess(true)
