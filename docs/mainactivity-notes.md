We were in the progress of migrating xml screens to compose. I want you to plan a migration for app/src/main/java/net/opendasharchive/openarchive/features/main/MainActivity.kt , app/src/main/java/net/opendasharchive/openarchive/features/main/ProjectAdapter.kt and app/src/main/java/net/
opendasharchive/openarchive/features/main/MainMediaFragment.kt including app/src/main/java/net/opendasharchive/openarchive/features/main/adapters/MainMediaAdapter.kt , app/src/main/java/net/opendasharchive/openarchive/features/main/adapters/MainMediaViewHolder.kt app/src/main/java/net/
opendasharchive/openarchive/features/main/SectionViewHolder.kt .

Here are some points to consider
The Screen contains a FolderBar at top with three modes (Default, Select, Edit)
folder bar is only visisble for project tab in botto bar
below that there is a viewpager2 with tabs for projects of a space (Space.current) and settingsfragment as last page
our app has sugar orm tables space and project, each space can have multiple projects
so at the activity level there should always be a selected space if available
if there are 4 projects in the selected space, the viewpager will have 4 tabs of MainMediaFragment(1 for each project) and Settings Fragment - totally 5 pages
the viewpager is swipable
there is a custom bottom navbar with 2 tabs (media, settings) and a fab plus btn at center
swiping viewpager will change the bottom navbar tabs and vice versa. 
in the main media fragment, there is an empty state view shown in three cases
1. no space - show welcome, tap to add server, arrow down
2. no projects in selected space - tap to add folder, arrow down
3. no media in the selected project - tap to add media, arrow down. - 
here selected project is the project that is visible in viewpager2

if viewpager is in settings fragment, the last index of a project is considered as selected project
if project has media, they all are fetched as collections and for each collection there will be a sectio header(timestamp-----section count)
and a 3 col media grid is displayed
pressing and holding a media item in main media fragment will trigger selection mode and select that media
this triggers folder bar to switch to selection mode (close, Select media ----- Remove)
folder bar at normal mode has these scenarios
 1.no space - shows nothing
 2.selected space has no projects - shows space icon only
 3.selected space has projects - shows space icon and project name, edit popup, total count in project)
edit popup has 4 options
rename folder - toggle folder bar to edit mode - in edit mode folder bar will have a close btn and a textfield filled and selected project name which can be modified and saved
select media - toggle folder bar to selection mode  - folder bar shows closebtn, Select media ----- Remove btn with icon) 
archive folder - archive current project
remove folder - remove folder confirm popup shown using dialogManger and removed
archive and remove will if successfull will remove the project from selectedSpace.projects - this will update the ui
if the space has another project it will be auto selected as current page and it's media will be shown
or no project - mainmediafagment emptyview scenraio 2 will be shown

mainactivity has a side navigation drawer on right side
the hamburger icon is only shown when bottom bar is in media tab
when no space is available, the drawer will be disabled
when there is space, drawer will be enabled
drawer will have a servers label at top with a arrow down, when we tap that it will show down a space list (all spaces in the app)
in the space list, Space.current(selectedspace/currentspace) will be highlighted
next to the server label expandable space list, there will be a divider
below divider current selected space with icon is shown
below that, with some space on left project list (folder icon and project name) on that space is shown
in folder list, the selected project icon will be highlighted
there will be a btn at bottom of drawer (+ New Folder) when tapped, close the drawer and navigate to add folder screen
in drawer project list, if a project is tapped, that will close the drawer, and in the viewpager, the page for that selected project will be displayed
in drawer space list, if a space is tapped, it will be the new selected space and viewpager will load pages for each project in that space and the side drawer will be closed and later will show projects of that space
the expandable space list when expanded slides over  the currentspace view and project list view smoothly(it does not push the other views down)

media fragment is a list of media grids and section headers where section header is a collection date and collection count, and media grid is the total media items
media list item is a square card view that will show media thumbnail
we dont have to worry about the inside of media list item now
based on the media status, the media grid list item will have different onclicks
if media.status = New/Local - navigate to PreviewMediaList scree
if media.status = Queued - open upload manager bottom sheet
user can press and hold on a media item to enter selection mode, in selection mode there will be a overlay over media list item(handled inside it -no need to worry)
this also toggle folder bar to selection mode, user can tap on other media to select them
user can tap on selected media to unselect them
if user has unselected all selected media item, the folder bar will be reset to normal mode

the bottom navbar add btn has following options
onClick = open gallery launcher - Picker.import where media items are copied to app cache directory and stored to db
onLongClick will open a content picker bottom sheet where there are three options - camera, gallery, file manager
alloptions will use Picker.import
MainActivity also have a option to import shared media - the idea is that we should be able to share files from other apps and import them into save app
if pager is in settings tab and if we tap/longpress on add btn, it will go back to previously selected project's main media fragment in pager and continue with the add btn action

queued media will be queried and uploaded to the space in a service called UploadService which is a JobService, 
from there BroadcastManager is used to send info to main media fragment while upload service is running and uploading files(media)
app/src/main/java/net/opendasharchive/openarchive/upload/BroadcastManager.kt is used for sending different info to mainmediafragment and update the ui
this is used to send if a media is started uploading (changed status from queue status to uploading status), then progress of upload(how many percentage uploaded)
finally uploaded or errored - for all things of upload status, these are shown in realtime inMainMediaFragment 
this is also used to send if media is deleted as well,
i would like a more modern coroutine compose friendly solution for this
upload service will upload all media items(from different projects, different spaces) all together based on media status = Queued
bt we only need upload related status info(uploading, percentage, uploaded, errored, etc) for the project that is selected(viewpager.current page)

when we start uploading in preview media screen and comes back to mainactivity, if we select a media item that is queued/uploaded, it will open the bottomsheet
which is inside mainactivity. 
when bottom sheet upload manger is opened, the upload service is paused
in there we can rearrange the media items, delete them. when some items are deleted and if we dismiss the bottom sheet, the upload service will resume and the main media fragment will have the updated media grid


in the compose migration, i aim to extract out all sugar orm related code into a repository layer (only in above MainActivity, MainMediaFragment, SettingsFragment)
i want solution on where to manage the space list, current space, projects in current space, selected space etc
the ui should react to the changes automatically like switching between projects and settings, switching between spaces
handling upload progress, media status etch should be reactive. once we do some change in Uploadmanagger bottom sheet, or in PreviewMediaScreen or the upload service is uploading and sending progress
or changin media status etc, these things should reflect to ui automatically
i want to move the folder bar from MainActivity to Mainmediafragment because the folder bar is not shown in settings fragment, and it is only related to a project
i want to know how we can reactively fetch data from db and update the ui automatically
we are planning to migrate to room db in future and move to full flow coroutine based architecture
bt we cant do it right now. bt the repository layer should be ready for that change
more info about room migration here - docs/room-migration.md
