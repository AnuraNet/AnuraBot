let currentlyChecked = 0;

window.addEventListener('load', function () {
    registerChangeListener();
});

function registerChangeListener() {
    document.querySelectorAll('input[type=checkbox]').forEach(function (value) {
        // Adding the event listener to all checkboxes
        value.addEventListener('change', changeListener);
        // Adding the pre checked boxes to the current count
        if (value.checked) {
            currentlyChecked++;
        }
    });
}

function changeListener(ev) {
    /** @namespace window.maxGames Number */
    // Preventing the check of another if the limit (maxGames) is reached
    if (ev.target.checked && currentlyChecked >= window.maxGames) {
        ev.target.checked = false;
        let gamesWord = window.maxGames !== 1 ? "games" : "game";
        alert("You can only select " + window.maxGames + " " + gamesWord + "!");
        return;
    }

    // Keeping track of number of the checked boxes
    if (ev.target.checked) {
        currentlyChecked++;
    } else {
        currentlyChecked--;
    }
}