package de.anura.bot.teamspeak

interface TimeChangedListener {

    /**
     * This listener will be called after the time for the
     * teamspeak user with the [uid] has been changed from
     * the [old] value to the [new] value.
     *
     * @param uid the affected user
     * @param old the time before the change
     * @param new the time after the change
     */
    fun changed(uid: String, old: Long, new: Long)

}