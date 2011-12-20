Todos = SC.Application.create();

// SUGGESTION: Your tags can be stored in a similar way as how todos are stored
Todos.Todo = SC.Object.extend({
  title: null,
  isDone: false
});

Todos.Tag = SC.Object.extend({
  name: null,
  prob: 0.0
});

Todos.todosController = SC.ArrayProxy.create({
  content: [],

  createTodo: function(title) {
    var todo = Todos.Todo.create({ title: title });
    this.pushObject(todo);
  },

  clearCompletedTodos: function() {
    this.filterProperty('isDone', true).forEach(this.removeObject, this);
  },

  remaining: function() {
    return this.filterProperty('isDone', false).get('length');
  }.property('@each.isDone'),

  allAreDone: function(key, value) {
    if (value !== undefined) {
      this.setEach('isDone', value);

      return value;
    } else {
      return !!this.get('length') && this.everyProperty('isDone', true);
    }
  }.property('@each.isDone')
});

Todos.tagsController = SC.ArrayProxy.create({
  content: [],

  createTag: function(name, prob) {
    var tag = Todos.Tag.create({ name: name, prob: prob });
	this.pushObject(tag);
  },

  clearAllTags: function() {
	var self = this;
	this.forEach(function(tag){
	  self.popObject();
	});
  }
});

Todos.StatsView = SC.View.extend({
  remainingBinding: 'Todos.todosController.remaining',

  remainingString: function() {
    var remaining = this.get('remaining');
    return remaining + (remaining === 1 ? " item" : " items");
  }.property('remaining')
});

// SUGGESTION: Think about using SC.TextArea to get a bigger input field
Todos.CreateTodoView = SC.TextField.extend({
  insertNewline: function() {
    var value = this.get('value');

    if (value) {
      Todos.todosController.createTodo(value);
      this.set('value', '');
      Todos.tagsController.clearAllTags();
    }
  },
  
  keyPress: function() {
	var value = this.get('value');
	
	if (value) {
	  $.get("recommend",
	    {
		  todo: value
	    },
	    function(data){
	      Todos.tagsController.beginPropertyChanges();
	      Todos.tagsController.clearAllTags();
	      data.forEach(function(item){
	    	Todos.tagsController.createTag(item.name, item.prob);  
	      });
	      Todos.tagsController.endPropertyChanges();
	    }
	  );
	}
  }
});

